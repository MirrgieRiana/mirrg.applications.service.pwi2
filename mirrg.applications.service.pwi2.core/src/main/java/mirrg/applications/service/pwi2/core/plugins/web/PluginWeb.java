package mirrg.applications.service.pwi2.core.plugins.web;

import java.net.InetSocketAddress;
import java.util.Hashtable;

import org.apache.commons.logging.LogFactory;

import mirrg.applications.service.pwi2.core.Message;
import mirrg.applications.service.pwi2.core.Source;
import mirrg.lithium.cgi.routing.CGIRouter;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.TerminalClosedException;
import mirrg.lithium.objectduct.inventories.Objectduct;
import mirrg.lithium.objectduct.logging.LoggerLater;
import mirrg.lithium.struct.ImmutableArray;
import net.arnx.jsonic.JSON;

public abstract class PluginWeb extends Objectduct
{

	public static final LoggerLater LOG = new LoggerLater(LogFactory.getLog(PluginWeb.class));

	//

	public abstract String getHostname();

	public abstract int getPortHttp();

	public abstract int getPortWebSocket();

	public abstract ImmutableArray<CGIRouter> getCGIRouters();

	private static class ReceivedData
	{

		public String name;
		public String text;

	}

	public PluginWeb()
	{
		registerWebCommandHandler("post", (remoteSocketAddress, command, argument) -> {
			ReceivedData receivedData = JSON.decode(argument, ReceivedData.class);
			Message message = new Message(
				new Source(
					"WEB(" + receivedData.name + ")",
					"#008800",
					remoteSocketAddress.getHostString()),
				receivedData.text);
			try {
				exporter.accept(message);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (TerminalClosedException e2) {
				LOG.warn(() -> message);
			}
		});
	}

	//

	private HttpServerPluginWeb httpServer;
	private WebSocketServerPluginWeb webSocketServer;
	private Terminal<Message> exporter;

	public HttpServerPluginWeb getHttpServer()
	{
		return httpServer;
	}

	public WebSocketServerPluginWeb getWebSocketServer()
	{
		return webSocketServer;
	}

	public Terminal<Message> getImporter()
	{
		return webSocketServer.getImporter();
	}

	public void setExporter(Terminal<Message> exporter)
	{
		this.exporter = exporter;
	}

	@Override
	protected void initInventories() throws Exception
	{
		add(httpServer = new HttpServerPluginWeb(this));
		add(webSocketServer = new WebSocketServerPluginWeb(this));
	}

	@Override
	protected void initConnections()
	{

	}

	@Override
	public void stop()
	{
		super.stop();
		exporter.close();
	}

	//

	private Hashtable<String, IWebCommandHandler> webCommandHandlers = new Hashtable<>();

	public void registerWebCommandHandler(String command, IWebCommandHandler webCommandHandler)
	{
		command = command.toLowerCase();
		if (webCommandHandlers.containsKey(command)) {
			throw new IllegalArgumentException("Duplicated Name: " + command);
		}
		webCommandHandlers.put(command, webCommandHandler);
	}

	public void onWebCommand(InetSocketAddress remoteSocketAddress, String message)
	{
		int index = message.indexOf(' ');
		if (index == -1) {
			onWebCommand(remoteSocketAddress, message, null);
		} else {
			onWebCommand(remoteSocketAddress, message.substring(0, index), message.substring(index + 1));
		}
	}

	public void onWebCommand(InetSocketAddress remoteSocketAddress, String command, String argument)
	{
		String command2 = command.toLowerCase();

		LOG.info(() -> "WebCommand (" + remoteSocketAddress + "): " + command2);
		LOG.debug(() -> "Argument: " + argument);

		if (webCommandHandlers.containsKey(command2)) {
			IWebCommandHandler webCommandHandler = webCommandHandlers.get(command2);
			try {
				webCommandHandler.accept(remoteSocketAddress, command, argument);
			} catch (Exception e) {
				LOG.error(() -> "Exception on WebCommand", () -> e);
			}
		} else {
			LOG.warn(() -> "Unknown WebCommand: " + command2);
		}
	}

}
