package mirrg.applications.service.pwi2.core.plugins.web;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.commons.logging.LogFactory;
import org.java_websocket.WebSocket;

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
		registerWebSocketCommandHandler("post", (connection, argument) -> {
			ReceivedData receivedData = JSON.decode(argument, ReceivedData.class);
			Message message = new Message(
				new Source(
					"WEB(" + receivedData.name + ")",
					"#008800",
					connection.getRemoteSocketAddress().getHostString()),
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

	public void stop() throws IOException, InterruptedException
	{
		httpServer.server.stop(0);
		webSocketServer.stop();
		exporter.close();
	}

	//

	private Hashtable<String, IWebSocketCommandHandler> webSocketCommandHandlers = new Hashtable<>();

	public void registerWebSocketCommandHandler(String command, IWebSocketCommandHandler webSocketCommandHandler)
	{
		command = command.toLowerCase();
		if (webSocketCommandHandlers.containsKey(command)) {
			throw new IllegalArgumentException("Duplicated Name: " + command);
		}
		webSocketCommandHandlers.put(command, webSocketCommandHandler);
	}

	public void onWebSocketCommand(WebSocket connection, String command, String argument)
	{
		String command2 = command.toLowerCase();
		if (webSocketCommandHandlers.containsKey(command2)) {
			webSocketCommandHandlers.get(command2).accept(connection, argument);
		} else {
			LOG.warn(() -> "Unknown WebSocketCommand: " + command2);
		}
	}

}
