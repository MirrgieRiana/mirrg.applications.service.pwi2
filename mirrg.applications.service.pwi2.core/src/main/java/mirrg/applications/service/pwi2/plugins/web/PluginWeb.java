package mirrg.applications.service.pwi2.plugins.web;

import java.io.IOException;

import mirrg.applications.service.pwi2.core.IExportBus;
import mirrg.applications.service.pwi2.core.IImportBus;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorHalfBlocking;
import mirrg.applications.service.pwi2.plugins.Message;
import mirrg.lithium.cgi.routing.CGIRouter;
import mirrg.lithium.struct.ImmutableArray;

public class PluginWeb
{

	public final String hostname;
	public final int portHttp;
	public final int portWebSocket;
	public final ImmutableArray<CGIRouter> cgiRouters;

	public final HttpServerPluginWeb httpServer;
	public final WebSocketServerPluginWeb webSocketServer;

	public PluginWeb(String hostname, int portHttp, int portWebSocket, ImmutableArray<CGIRouter> cgiRouters) throws IOException
	{
		this.hostname = hostname;
		this.portHttp = portHttp;
		this.portWebSocket = portWebSocket;
		this.cgiRouters = cgiRouters;

		httpServer = new HttpServerPluginWeb(this);
		webSocketServer = new WebSocketServerPluginWeb(this);
	}

	public void start()
	{
		httpServer.server.start();
		System.err.println("HTTP Server Start: http://" + hostname + ":" + portHttp);
		webSocketServer.start();
		System.err.println("WebSocket Server Start: http://" + hostname + ":" + portWebSocket);
	}

	public void stop() throws IOException, InterruptedException
	{
		httpServer.server.stop(0);
		webSocketServer.stop();
	}

	public IImportBus<IAcceptorHalfBlocking<Message>> getImportBus()
	{
		return webSocketServer.getImportBus();
	}

	public IExportBus<IAcceptorHalfBlocking<Message>> getExportBus()
	{
		return webSocketServer.getExportBus();
	}

}
