package mirrg.applications.service.pwi2;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import com.sun.net.httpserver.BasicAuthenticator;

import mirrg.applications.service.pwi2.core.Message;
import mirrg.applications.service.pwi2.core.Source;
import mirrg.applications.service.pwi2.core.inventories.DaemonReaderHopper;
import mirrg.applications.service.pwi2.core.plugins.process.PluginProcess;
import mirrg.applications.service.pwi2.core.plugins.web.PluginWeb;
import mirrg.lithium.cgi.routing.CGIRouter;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.TerminalClosedException;
import mirrg.lithium.objectduct.inventories.BusedHopper;
import mirrg.lithium.objectduct.inventories.Objectduct;
import mirrg.lithium.objectduct.logging.AppenderObjectduct;
import mirrg.lithium.objectduct.logging.LoggerLater;
import mirrg.lithium.struct.ImmutableArray;

public abstract class ObjectductPwi2 extends Objectduct
{

	public static final LoggerLater LOG = new LoggerLater(LogFactory.getLog(ObjectductPwi2.class));

	public AutoRestarter autoRestarter = new AutoRestarter(new Runnable() {

		@Override
		public void run()
		{
			try {
				if (!pluginProcess.isRunning()) up();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	});

	{
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			@Override
			public void run()
			{
				exit();
			}

		}));
	}

	public FileIncrementer fileIncrementer;
	public int sessionId;

	//

	public abstract String getHostname();

	public abstract int getPortHttp();

	public abstract int getPortWebSocket();

	public abstract ArrayList<CGIRouter> getCgiRouters();

	public abstract Optional<Pattern> getAuthenticationRegex();

	public abstract String[] getCommand();

	public abstract File getCurrentDirectory();

	public abstract int getMaxLength();

	public abstract File getProcessCounterFile();

	//

	public BusedHopper<Message> hopperInput;
	public DaemonReaderHopper daemonReaderHopper;
	public PluginWeb pluginWeb;
	public PluginProcess pluginProcess;
	public BusedHopper<Message> hopperOutput;

	@Override
	protected void initInventories() throws Exception
	{
		add(hopperInput = new BusedHopper<>(100));
		add(daemonReaderHopper = new DaemonReaderHopper(new InputStreamReader(System.in)));
		add(pluginWeb = new PluginWeb() {

			@Override
			public String getHostname()
			{
				return ObjectductPwi2.this.getHostname();
			}

			@Override
			public int getPortHttp()
			{
				return ObjectductPwi2.this.getPortHttp();
			}

			@Override
			public int getPortWebSocket()
			{
				return ObjectductPwi2.this.getPortWebSocket();
			}

			@Override
			public ImmutableArray<CGIRouter> getCGIRouters()
			{
				return new ImmutableArray<>(getCgiRouters());
			}

		});
		add(pluginProcess = new PluginProcess() {

			@Override
			protected String[] getCommand()
			{
				return ObjectductPwi2.this.getCommand();
			}

			@Override
			protected File getCurrentDirectory()
			{
				return ObjectductPwi2.this.getCurrentDirectory();
			}

			@Override
			protected int getMaxLength()
			{
				return ObjectductPwi2.this.getMaxLength();
			}

		});
		add(hopperOutput = new BusedHopper<>(100));
	}

	@Override
	protected void initConnections()
	{
		fileIncrementer = new FileIncrementer(getProcessCounterFile());
		pluginWeb.getHttpServer().setAuthenticator(new BasicAuthenticator("WebInterface") {

			@Override
			public boolean checkCredentials(String username, String password)
			{
				return getAuthenticationRegex()
					.map(p -> !username.contains(":") && p.matcher(username + ":" + password).matches())
					.orElse(true);
			}

		});
		pluginWeb.registerWebCommandHandler("auto_restart", (remoteSocketAddress, command, argument) -> {
			if ("true".equals(argument)) {
				autoRestarter.up();
			} else if ("false".equals(argument)) {
				autoRestarter.down();
			} else {
				PluginWeb.LOG.warn(() -> "Illegal argument: " + "auto_restart " + argument);
			}
		});
		pluginWeb.registerWebCommandHandler("start", (remoteSocketAddress, command, argument) -> {
			try {
				if (pluginProcess.isRunning()) {
					PluginProcess.LOG.warn(() -> "Process is already running!");
					return;
				}
				up();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		pluginWeb.registerWebCommandHandler("stop", (remoteSocketAddress, command, argument) -> {
			if (!pluginProcess.isRunning()) {
				PluginProcess.LOG.warn(() -> "No process is running!");
				return;
			}
			pluginProcess.down();
		});
		pluginWeb.registerWebCommandHandler("exit", (remoteSocketAddress, command, argument) -> {
			new Thread(() -> System.exit(0)).start();
		});

		//

		AppenderObjectduct.setExporter(hopperOutput.getImportBus().addImporter()
			.map(new Function<LoggingEvent, Message>() {

				@Override
				public Message apply(LoggingEvent e)
				{
					String color;
					if (e.getLevel() == Level.FATAL) {
						color = "#ff00ff";
					} else if (e.getLevel() == Level.ERROR) {
						color = "#ff00ff";
					} else if (e.getLevel() == Level.WARN) {
						color = "#aa00aa";
					} else if (e.getLevel() == Level.INFO) {
						color = "#8888ff";
					} else if (e.getLevel() == Level.DEBUG) {
						color = "#888888";
					} else if (e.getLevel() == Level.TRACE) {
						color = "#aaaaaa";
					} else {
						color = "#aa00aa";
					}

					return new Message(
						new Source(
							"SYSTEM(" + e.getLevel() + ")",
							color,
							"" + e.getThreadName() + "/" + shorten(e.getLoggerName())),
						e.getMessage().toString());
				}

				private String shorten(String loggerName)
				{
					int index = loggerName.lastIndexOf('.');
					return index == -1 ? loggerName : loggerName.substring(index + 1);
				}

			})
			.filter(e -> !e.getLoggerName().equals("save")));
		hopperInput.getExportBus().addExporter(pluginProcess.getImporter()
			.map(m -> m.text));
		hopperInput.getExportBus().addExporter(hopperOutput.getImportBus().addImporter());
		daemonReaderHopper.setExporter(hopperInput.getImportBus().addImporter()
			.map(t -> new Message(new Source("STDIN", "#0000ff", ""), t.y, t.x)));
		pluginWeb.setExporter(hopperInput.getImportBus().addImporter());
		pluginProcess.setExporter(hopperOutput.getImportBus().addImporter());
		hopperOutput.getExportBus().addExporter(pluginWeb.getImporter());
		hopperOutput.getExportBus().addExporter(new Terminal<Message>() {

			private Log LOG_SAVE = LogFactory.getLog("save");

			@Override
			protected void acceptImpl(Message t) throws InterruptedException, TerminalClosedException
			{
				LOG_SAVE.info(t);
			}

			@Override
			protected void closeImpl()
			{

			}

		});
	}

	public void up() throws Exception
	{
		sessionId = fileIncrementer.next();
		pluginProcess.up();
	}

	private void exit()
	{
		LOG.info(() -> "Stopping...");
		autoRestarter.down();
		stop();
		try {
			join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
