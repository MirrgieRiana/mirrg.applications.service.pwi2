package mirrg.applications.service.pwi2;

import java.net.InetSocketAddress;
import java.time.Instant;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import net.arnx.jsonic.JSON;

public class PWI2WebSocketServer extends WebSocketServer
{

	private PWI2 pwi2;

	public PWI2WebSocketServer(PWI2 pwi2, InetSocketAddress address)
	{
		super(address);
		this.pwi2 = pwi2;

		pwi2.event().register(PWI2Event.AddMessage.class, e -> {
			connections().forEach(c -> sendMessage(c, e.message));
		});
	}

	@Override
	public void onStart()
	{

	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake)
	{
		for (PWI2IndexedMessage message : pwi2.getMessages()) {
			sendMessage(conn, message);
		}
	}

	public static void sendMessage(WebSocket conn, PWI2IndexedMessage message)
	{
		conn.send("Message " + JSON.encode(message));
	}

	private class PostData
	{

		public String name;
		public String text;

	}

	@Override
	public void onMessage(WebSocket conn, String message)
	{
		if (message.startsWith("Post ")) {
			PostData postData = JSON.decode(message.substring(5), PostData.class);
			pwi2.addMessage(new PWI2Message(
				Instant.now(),
				new PWI2Source(
					"WEB(" + postData.name + ")",
					"#008800",
					conn.getRemoteSocketAddress().getHostString()),
				null,
				postData.text));
		}
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{

	}

	@Override
	public void onError(WebSocket conn, Exception ex)
	{
		ex.printStackTrace();
	}

}
