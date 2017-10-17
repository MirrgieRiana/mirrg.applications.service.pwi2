package mirrg.applications.service.pwi2.core.plugins.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import mirrg.lithium.cgi.HTTPResponse;
import mirrg.lithium.cgi.routing.CGIRouter;
import mirrg.lithium.cgi.routing.HttpHandlerCGIRouting;
import mirrg.lithium.objectduct.inventories.Objectduct;

public class HttpServerPluginWeb extends Objectduct
{

	private PluginWeb plugin;
	public final HttpServer server;
	public final HttpContext contextMain;

	private ArrayList<HttpContext> contexts = new ArrayList<>();

	public HttpServerPluginWeb(PluginWeb plugin) throws IOException
	{
		this.plugin = plugin;
		this.server = HttpServer.create(new InetSocketAddress(plugin.getHostname(), plugin.getPortHttp()), 10);
		this.contextMain = server.createContext("/", new HttpHandlerCGIRouting(plugin.getCGIRouters().toArray(CGIRouter[]::new)));

		contexts.add(contextMain);
		contexts.add(server.createContext("/__api/get/portWebSocket",
			e -> HTTPResponse.send(e, 200, "" + plugin.getPortWebSocket())));
		contexts.add(server.createContext("/__api/get/basicAuthenticationName",
			e -> HTTPResponse.send(e, 200, "" + (e.getPrincipal() == null ? "" : e.getPrincipal().getUsername()))));
		contexts.add(server.createContext("/__api/webCommand",
			e -> {
				String query = e.getRequestURI().getQuery();
				if (query != null) {
					plugin.onWebCommand(e.getRemoteAddress(), query);
				}
				HTTPResponse.send(e, 200, "");
			}));
	}

	public void setAuthenticator(Authenticator authenticator)
	{
		contexts.forEach(c -> c.setAuthenticator(authenticator));
	}

	@Override
	protected void initInventories()
	{

	}

	@Override
	protected void initConnections()
	{

	}

	@Override
	public void start() throws Exception
	{
		super.start();
		server.start();
		PluginWeb.LOG.info(() -> "HTTP Server Start: http://" + plugin.getHostname() + ":" + plugin.getPortHttp());
	}

	@Override
	public void stop()
	{
		super.stop();
		server.stop(0);
	}

}
