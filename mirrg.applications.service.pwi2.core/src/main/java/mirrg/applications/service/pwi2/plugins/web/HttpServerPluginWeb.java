package mirrg.applications.service.pwi2.plugins.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import mirrg.lithium.cgi.HTTPResponse;
import mirrg.lithium.cgi.routing.CGIRouter;
import mirrg.lithium.cgi.routing.HttpHandlerCGIRouting;

public class HttpServerPluginWeb
{

	@SuppressWarnings("unused")
	private PluginWeb plugin;
	public final HttpServer server;
	public final HttpContext contextMain;

	private ArrayList<HttpContext> contexts = new ArrayList<>();

	public HttpServerPluginWeb(PluginWeb plugin) throws IOException
	{
		this.plugin = plugin;
		this.server = HttpServer.create(new InetSocketAddress(plugin.hostname, plugin.portHttp), 10);
		this.contextMain = server.createContext("/", new HttpHandlerCGIRouting(plugin.cgiRouters.toArray(CGIRouter[]::new)));

		contexts.add(contextMain);
		contexts.add(server.createContext("/__api/get/portWebSocket",
			e -> HTTPResponse.send(e, 200, "" + plugin.portWebSocket)));
		contexts.add(server.createContext("/__api/get/basicAuthenticationName",
			e -> HTTPResponse.send(e, 200, "" + (e.getPrincipal() == null ? "" : e.getPrincipal().getUsername()))));
	}

	public void setAuthenticator(Authenticator authenticator)
	{
		contexts.forEach(c -> c.setAuthenticator(authenticator));
	}

}
