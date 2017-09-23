package mirrg.applications.service.pwi2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;

import mirrg.lithium.cgi.routing.CGIRouter;
import mirrg.lithium.event.EventManager;

public class PWI2
{

	public final String hostname;
	public final int portHttp;
	public final int portWebSocket;
	public final int messageQueueSize;

	private ArrayDeque<PWI2IndexedMessage> messageQueue;
	private int messageIndexNext = 0;

	public PWI2(String hostname, int portHttp, int portWebSocket, int messageQueueSize)
	{
		this.hostname = hostname;
		this.portHttp = portHttp;
		this.portWebSocket = portWebSocket;
		this.messageQueueSize = messageQueueSize;
		this.messageQueue = new ArrayDeque<>(messageQueueSize + 1);
	}

	//

	public void addMessage(PWI2Message message)
	{
		PWI2IndexedMessage message2;
		synchronized (this) {
			message2 = new PWI2IndexedMessage(messageIndexNext, message);
			messageQueue.add(message2);
			while (messageQueue.size() > messageQueueSize) {
				messageQueue.removeFirst();
			}
			messageIndexNext++;
		}

		event().post(new PWI2Event.AddMessage(message2));
	}

	public synchronized PWI2IndexedMessage[] getMessages()
	{
		return messageQueue.toArray(new PWI2IndexedMessage[0]);
	}

	//

	private PWI2HttpServer httpServer;

	public PWI2HttpServer getHttpServer(CGIRouter[] cgiRouters) throws IOException
	{
		if (httpServer == null) {
			httpServer = new PWI2HttpServer(this, new InetSocketAddress(hostname, portHttp), cgiRouters);
		}
		return httpServer;
	}

	private PWI2WebSocketServer webSocketServer;

	public PWI2WebSocketServer getWebSocketServer()
	{
		if (webSocketServer == null) {
			webSocketServer = new PWI2WebSocketServer(this, new InetSocketAddress(hostname, portWebSocket));
		}
		return webSocketServer;
	}

	//

	private EventManager<PWI2Event> event = new EventManager<>();

	public EventManager<PWI2Event> event()
	{
		return event;
	}

}
