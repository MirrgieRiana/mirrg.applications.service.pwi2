package mirrg.applications.service.pwi2;

import java.io.File;
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
import mirrg.applications.service.pwi2.core.plugins.process.PluginProcess;
import mirrg.applications.service.pwi2.core.plugins.web.PluginWeb;
import mirrg.lithium.cgi.routing.CGIRouter;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.TerminalClosedException;
import mirrg.lithium.objectduct.inventories.BusedHopper;
import mirrg.lithium.objectduct.inventories.Objectduct;
import mirrg.lithium.objectduct.logging.AppenderObjectduct;
import mirrg.lithium.struct.ImmutableArray;

public abstract class ObjectductPwi2 extends Objectduct
{

	public abstract String getHostname();

	public abstract int getPortHttp();

	public abstract int getPortWebSocket();

	public abstract ArrayList<CGIRouter> getCgiRouters();

	public abstract Optional<Pattern> getAuthenticationRegex();

	public abstract String[] getCommand();

	public abstract File getCurrentDirectory();

	public abstract int getMaxLength();

	//

	public BusedHopper<Message> hopperInput;
	public PluginWeb pluginWeb;
	public PluginProcess pluginProcess;
	public BusedHopper<Message> hopperOutput;

	@Override
	protected void initInventories() throws Exception
	{
		add(hopperInput = new BusedHopper<>(100));
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
		pluginWeb.getHttpServer().setAuthenticator(new BasicAuthenticator("WebInterface") {

			@Override
			public boolean checkCredentials(String username, String password)
			{
				return getAuthenticationRegex()
					.map(p -> !username.contains(":") && p.matcher(username + ":" + password).matches())
					.orElse(true);
			}

		});
		pluginWeb.registerWebSocketCommandHandler("start", (connection, argument) -> {
			try {
				pluginProcess.up();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		pluginWeb.registerWebSocketCommandHandler("stop", (connection, argument) -> {
			pluginProcess.down();
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
						new Source("SYSTEM(" + e.getLevel() + ")", color, "" + e.getThreadName()),
						e.getMessage().toString());
				}

			})
			.filter(e -> !e.getLoggerName().equals("save")));
		hopperInput.getExportBus().addExporter(pluginProcess.getImporter()
			.map(m -> m.text));
		hopperInput.getExportBus().addExporter(hopperOutput.getImportBus().addImporter());
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

}
