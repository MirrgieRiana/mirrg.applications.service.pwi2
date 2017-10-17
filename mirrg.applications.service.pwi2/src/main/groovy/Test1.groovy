import java.util.regex.Pattern

import mirrg.applications.service.pwi2.ObjectductPwi2
import mirrg.applications.service.pwi2.core.plugins.web.PluginWeb
import mirrg.lithium.cgi.CGIBufferPool
import mirrg.lithium.cgi.CGIServerSetting
import mirrg.lithium.cgi.ILogger
import mirrg.lithium.cgi.routing.CGIPattern
import mirrg.lithium.cgi.routing.CGIRouter

class ObjectductPwi2Test1 extends ObjectductPwi2 {
	String getHostname() {
		"0.0.0.0"
	}
	int getPortHttp() {
		3030
	}
	int getPortWebSocket() {
		3031
	}
	ArrayList<CGIRouter> getCgiRouters() {
		def cgiRouters = [];
		ILogger logger = new ILogger() {
					void accept(String message) {
						PluginWeb.LOG.warn({ message });
					}
					void accept(Exception e) {
						PluginWeb.LOG.warn({ "" },{ e });
					}
				};
		CGIBufferPool cgiBufferPool = new CGIBufferPool(1000000, 1000000, 10, 10000);
		({
			CGIRouter cgiRouter = new CGIRouter(new CGIServerSetting(
					getPortHttp(),
					"WebInterface",
					new File("http_home"),
					5000,
					cgiBufferPool));
			cgiRouter.addIndex("index.html");
			cgiRouter.addIndex("index.pl");
			cgiRouter.addIndex("index.php");
			cgiRouter.addCGIPattern(new CGIPattern(
					".pl",
					["perl", "%s"] as String[],
					logger,
					1000));
			cgiRouter.addCGIPattern(new CGIPattern(
					".php",
					["php-cgi", "%s"] as String[],
					logger,
					1000));
			cgiRouters.add(cgiRouter);
		})()
		cgiRouters
	}

	Optional<Pattern> getAuthenticationRegex() {
		Optional.of(Pattern.compile("[a-zA-Z0-9_]{1,16}:ho-tyo- tou4rou"))
	}

	String[] getCommand() {
		["perl", "test.pl"] as String[]
	}
	File getCurrentDirectory() {
		new File(".")
	}
	int getMaxLength() {
		200
	}
}

try {
	def objectduct = new ObjectductPwi2Test1()
	objectduct.autoRestarter.up()
	objectduct.init()
	objectduct.start()
} catch (Exception e) {
	e.printStackTrace()
	System.exit(1)
}