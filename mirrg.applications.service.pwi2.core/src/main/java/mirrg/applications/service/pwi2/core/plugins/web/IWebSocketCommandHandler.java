package mirrg.applications.service.pwi2.core.plugins.web;

import org.java_websocket.WebSocket;

public interface IWebSocketCommandHandler
{

	public void accept(WebSocket connection, String argument);

}
