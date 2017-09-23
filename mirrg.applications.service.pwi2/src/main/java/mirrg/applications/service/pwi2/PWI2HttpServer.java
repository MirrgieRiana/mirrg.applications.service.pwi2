package mirrg.applications.service.pwi2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import mirrg.lithium.cgi.HTTPResponse;
import mirrg.lithium.cgi.routing.CGIRouter;
import mirrg.lithium.cgi.routing.HttpHandlerCGIRouting;

public class PWI2HttpServer
{

	@SuppressWarnings("unused")
	private PWI2 pwi2;

	public final HttpServer server;
	public final HttpContext contextMain;

	private ArrayList<HttpContext> contexts = new ArrayList<>();

	public PWI2HttpServer(PWI2 pwi2, InetSocketAddress address, CGIRouter[] cgiRouters) throws IOException
	{
		this.pwi2 = pwi2;
		this.server = HttpServer.create(address, 10);
		this.contextMain = server.createContext("/", new HttpHandlerCGIRouting(cgiRouters));
		contexts.add(contextMain);
		contexts.add(server.createContext("/__api/get/portWebSocket",
			e -> HTTPResponse.send(e, 200, "" + pwi2.portWebSocket)));
		contexts.add(server.createContext("/__api/get/basicAuthenticationName",
			e -> HTTPResponse.send(e, 200, "" + (e.getPrincipal() == null ? "" : e.getPrincipal().getUsername()))));
	}

	public void setAuthenticator(Authenticator authenticator)
	{
		contexts.forEach(c -> c.setAuthenticator(authenticator));
	}

}
