package mirrg.applications.service.pwi2;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import groovy.lang.GroovyShell;
import mirrg.applications.service.pwi2.web.ConfigureCGIApplication;
import mirrg.applications.service.pwi2.web.ConfigureWeb;
import mirrg.lithium.cgi.CGIBufferPool;
import mirrg.lithium.cgi.CGIPattern;
import mirrg.lithium.cgi.CGIRouter;
import mirrg.lithium.cgi.CGIRouter.EnumRouteResult;
import mirrg.lithium.cgi.CGIServerSetting;
import mirrg.lithium.cgi.HTTPResponse;
import mirrg.lithium.cgi.ILogger;
import mirrg.lithium.struct.Tuple;

public class Main
{

	public static void main(String[] args) throws IOException
	{
		File propertyFile = new File("pwi2.groovy");
		Optional<String> initializationGroovyScript = Optional.empty();

		try {
			int i = 0;
			while (i < args.length) {
				if (args[i].equals("-p")) {
					i++;
					propertyFile = new File(args[i]);
				} else if (args[i].equals("-g")) {
					i++;
					initializationGroovyScript = Optional.of(args[i]);
				} else {
					throw null;
				}
			}
		} catch (RuntimeException e) {
			System.err.println("USAGE: [-p propertyFile] [-g initializationGroovyScript]");
		}

		main(propertyFile, initializationGroovyScript);

	}

	public static void main(File propertyFile, Optional<String> initializationGroovyScript) throws IOException
	{
		GroovyShell groovyShell = new GroovyShell();
		ConfigurePWI2 configure = new ConfigurePWI2();
		groovyShell.setVariable("configure", configure);
		groovyShell.run(propertyFile, new String[0]);
		if (initializationGroovyScript.isPresent()) groovyShell.evaluate(initializationGroovyScript.get());

		main(configure);
	}

	public static void main(ConfigurePWI2 configure) throws IOException
	{
		startHTTPServer(configure.web);
	}

	private static void startHTTPServer(ConfigureWeb configureWeb) throws IOException
	{
		CGIBufferPool cgiBufferPool = new CGIBufferPool(
			configureWeb.requestBufferSize,
			configureWeb.responseBufferSize,
			10,
			10000);

		ILogger logger = new ILogger() {
			@Override
			public void accept(String message)
			{
				System.err.println(message);
			}

			@Override
			public void accept(Exception e)
			{
				e.printStackTrace();
			}
		};

		ArrayList<CGIRouter> cgiRouters = configureWeb.cgiRoutings.stream()
			.map(r -> {
				CGIRouter cgiRouter = new CGIRouter(new CGIServerSetting(
					configureWeb.port,
					r.serverName,
					r.documentRoot,
					configureWeb.timeoutMs,
					cgiBufferPool));
				for (String index : r.indexes) {
					cgiRouter.addIndex(index);
				}
				for (ConfigureCGIApplication cgiApplication : r.cgiApplications) {
					cgiRouter.addCGIPattern(new CGIPattern(
						cgiApplication.extension,
						cgiApplication.commandFormat,
						logger,
						1000));
				}
				return cgiRouter;
			})
			.collect(Collectors.toCollection(ArrayList::new));

		HttpServer httpServer = HttpServer.create(new InetSocketAddress(configureWeb.host, configureWeb.port), configureWeb.backlog);
		{
			HttpContext context = httpServer.createContext("/", e -> {
				new Thread(() -> {
					try {
						for (CGIRouter cgiRouter : cgiRouters) {
							Tuple<EnumRouteResult, HTTPResponse> result = cgiRouter.route(e.getRequestURI().getPath());
							if (result.x.found) throw result.y;
						}
						throw HTTPResponse.get(404);
					} catch (HTTPResponse httpResponse) {
						try {
							httpResponse.sendResponse(e);
							return;
						} catch (IOException e1) {
							try {
								HTTPResponse.get(500).sendResponse(e);
								return;
							} catch (IOException e2) {
								e2.addSuppressed(e1);
								e2.printStackTrace();
							}
						}
					}
				}).start();
			});
			if (configureWeb.authenticator.isPresent()) {
				context.setAuthenticator(new BasicAuthenticator("WebInterface") {
					@Override
					public boolean checkCredentials(String arg0, String arg1)
					{
						return configureWeb.authenticator.get().test(arg0, arg1);
					}
				});
			}
		}

		httpServer.start();
		System.err.println("Server Start: http://" + configureWeb.host + ":" + configureWeb.port);
	}

}
