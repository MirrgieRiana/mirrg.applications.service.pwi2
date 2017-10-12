package mirrg.applications.service.pwi2.plugins.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Hashtable;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import mirrg.applications.service.pwi2.core.IExportBus;
import mirrg.applications.service.pwi2.core.IImportBus;
import mirrg.applications.service.pwi2.core.ITerminal;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorHalfBlocking;
import mirrg.applications.service.pwi2.core.containers.BufferedHopper;
import mirrg.applications.service.pwi2.core.containers.Cache;
import mirrg.applications.service.pwi2.core.containers.Hopper;
import mirrg.applications.service.pwi2.plugins.IndexedMessage;
import mirrg.applications.service.pwi2.plugins.Message;
import mirrg.applications.service.pwi2.plugins.Source;
import net.arnx.jsonic.JSON;

public class WebSocketServerPluginWeb
{

	@SuppressWarnings("unused")
	private PluginWeb plugin;
	public final WebSocketServer server;

	public WebSocketServerPluginWeb(PluginWeb plugin)
	{
		this.plugin = plugin;
		this.server = new WebSocketServer(new InetSocketAddress(plugin.hostname, plugin.portWebSocket)) {

			@Override
			public void onStart()
			{

			}

			@Override
			public void onOpen(WebSocket conn, ClientHandshake handshake)
			{
				synchronized (lock) {
					ConnectionWrapper connectionWrapper = new ConnectionWrapper(conn);
					connectionWrapper.start();
					connectionWrappers.put(conn, connectionWrapper);

					try {
						connectionWrapper.accept(cache.toArray(IndexedMessage[]::new));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			class ReceivedData
			{

				public String name;
				public String text;

			}

			@Override
			public void onMessage(WebSocket conn, String message)
			{
				if (message.startsWith("Post ")) {
					ReceivedData receivedData = JSON.decode(message.substring(5), ReceivedData.class);
					try {
						exporter.getAcceptor().accept(new Message(
							new Source(
								"WEB(" + receivedData.name + ")",
								"#008800",
								conn.getRemoteSocketAddress().getHostString()),
							null,
							receivedData.text));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onClose(WebSocket conn, int code, String reason, boolean remote)
			{
				synchronized (lock) {
					ConnectionWrapper connectionWrapper = connectionWrappers.get(conn);
					connectionWrapper.stop();
					connectionWrappers.remove(conn);
				}
			}

			@Override
			public void onError(WebSocket conn, Exception ex)
			{
				ex.printStackTrace();
			}
		};

		initHoppers();
	}

	//

	private Object lock = new Object();

	private BufferedHopper<Message> hopperIn;
	private Cache<IndexedMessage> cache;
	private Hopper<Message, IAcceptorHalfBlocking<Message>> hopperOut;

	private int index = 0;
	private Hashtable<WebSocket, ConnectionWrapper> connectionWrappers = new Hashtable<>();
	private ITerminal<IAcceptorHalfBlocking<Message>> exporter;

	private void initHoppers()
	{

		// コンテナ
		hopperIn = new BufferedHopper<>(1000);
		cache = new Cache<>(2000);
		hopperOut = new Hopper<>(IAcceptorHalfBlocking.getTransformer());

		// コネクション
		hopperIn.addExporter(() -> m -> {
			synchronized (lock) {
				IndexedMessage indexedMessage = new IndexedMessage(index, m);
				index++;

				cache.accept(indexedMessage);

				for (ConnectionWrapper connectionWrapper : connectionWrappers.values()) {
					connectionWrapper.accept(indexedMessage);
				}

			}
		});
		exporter = hopperOut.addImporter();

	}

	public void start()
	{
		server.start();
		hopperIn.start();
		hopperOut.start();
	}

	public void stop() throws IOException, InterruptedException
	{
		server.stop();
		exporter.close();
	}

	public IImportBus<IAcceptorHalfBlocking<Message>> getImportBus()
	{
		return hopperIn.getImportBus();
	}

	public IExportBus<IAcceptorHalfBlocking<Message>> getExportBus()
	{
		return hopperOut.getExportBus();
	}

}
