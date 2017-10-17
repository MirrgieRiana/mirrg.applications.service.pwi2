package mirrg.applications.service.pwi2.core.plugins.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Hashtable;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import mirrg.applications.service.pwi2.core.IndexedMessage;
import mirrg.applications.service.pwi2.core.Message;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.TerminalClosedException;
import mirrg.lithium.objectduct.inventories.Cache;
import mirrg.lithium.objectduct.inventories.Hopper;
import mirrg.lithium.objectduct.inventories.Objectduct;

public class WebSocketServerPluginWeb extends Objectduct
{

	private PluginWeb plugin;
	public final WebSocketServer server;
	private Object lock = new Object();
	private Hashtable<WebSocket, ObjectductWebSocket> objectductWebSockets = new Hashtable<>();
	private int index = 0;

	public WebSocketServerPluginWeb(PluginWeb plugin)
	{
		this.plugin = plugin;
		this.server = new WebSocketServer(new InetSocketAddress(plugin.getHostname(), plugin.getPortWebSocket())) {

			@Override
			public void onStart()
			{

			}

			@Override
			public void onOpen(WebSocket conn, ClientHandshake handshake)
			{
				synchronized (lock) {
					// ラッパーを登録
					ObjectductWebSocket objectductWebSocket = new ObjectductWebSocket(conn);
					try {
						objectductWebSocket.init();
						objectductWebSocket.start();
					} catch (Exception e) {
						conn.close();
						throw new RuntimeException(e);
					}
					objectductWebSockets.put(conn, objectductWebSocket);

					// これまでのメッセージを全て送信
					try {
						objectductWebSocket.getImporter().accept(cache.toArray(IndexedMessage[]::new));
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} catch (TerminalClosedException e) {
						PluginWeb.LOG.warn(() -> "", () -> e);
					}
				}
			}

			@Override
			public void onMessage(WebSocket conn, String message)
			{
				plugin.onWebCommand(conn.getRemoteSocketAddress(), message);
			}

			@Override
			public void onClose(WebSocket conn, int code, String reason, boolean remote)
			{
				synchronized (lock) {
					// ラッパーの登録を解除
					ObjectductWebSocket objectductWebSocket = objectductWebSockets.get(conn);
					objectductWebSocket.getImporter().close();
					objectductWebSockets.remove(conn);
				}
			}

			@Override
			public void onError(WebSocket conn, Exception ex)
			{
				ex.printStackTrace();
			}
		};
	}

	//

	private Hopper<Message> hopperIn;
	private Cache<IndexedMessage> cache;

	public Terminal<Message> getImporter()
	{
		return hopperIn.getImporter();
	}

	@Override
	protected void initInventories()
	{
		add(hopperIn = new Hopper<>(1000));
		add(cache = new Cache<>(2000));
	}

	@Override
	protected void initConnections()
	{
		hopperIn.setExporter(new Terminal<Message>() {

			@Override
			protected void acceptImpl(Message message) throws InterruptedException, TerminalClosedException
			{
				synchronized (lock) {
					IndexedMessage indexedMessage = new IndexedMessage(index, message);
					index++;

					cache.getImporter().accept(indexedMessage);

					for (ObjectductWebSocket objectductWebSocket : objectductWebSockets.values()) {
						objectductWebSocket.getImporter().accept(new IndexedMessage[] { indexedMessage });
					}

				}
			}

			@Override
			protected void closeImpl()
			{
				PluginWeb.LOG.info(() -> "Input closed.");
			}

		});
	}

	@Override
	public void start() throws Exception
	{
		super.start();
		server.start();
		PluginWeb.LOG.info(() -> "WebSocket Server Start: http://" + plugin.getHostname() + ":" + plugin.getPortWebSocket());
	}

	@Override
	public void stop()
	{
		super.stop();
		try {
			server.stop();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {

		}
	}

}
