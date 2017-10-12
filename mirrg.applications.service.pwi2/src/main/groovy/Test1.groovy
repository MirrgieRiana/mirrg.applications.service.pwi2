import com.sun.net.httpserver.*

import mirrg.applications.service.pwi2.core.*
import mirrg.applications.service.pwi2.core.acceptors.*
import mirrg.applications.service.pwi2.core.containers.*
import mirrg.applications.service.pwi2.plugins.*
import mirrg.applications.service.pwi2.plugins.process.*
import mirrg.applications.service.pwi2.plugins.web.*
import mirrg.lithium.cgi.*
import mirrg.lithium.cgi.routing.*
import mirrg.lithium.struct.*

def cgiBufferPool = new CGIBufferPool(1000000, 1000000, 10, 10000)
def hostname = "0.0.0.0"
def portHttp = 3030
def portWebSocket = 3031
def logger = new ILogger() {
			@Override
			public void accept(String message) {
				System.err.println(message);
			}
			@Override
			public void accept(Exception e) {
				e.printStackTrace();
			}
		}
def cgiRouters = []
cgiRouters << {
	def cgiRouter = new CGIRouter(new CGIServerSetting(portHttp, "WebInterface", new File("http_home"), 5000, cgiBufferPool))
	cgiRouter.addIndex "index.html"
	cgiRouter.addIndex "index.pl"
	cgiRouter.addCGIPattern new CGIPattern(".pl", ["perl", "%s"] as String[], logger, 1000)
	cgiRouter
}()

//

// コンテナ宣言
def hopperOutputBus = new Hopper<>(IAcceptorHalfBlocking.getTransformer())
def hopperInputBus = new Hopper<>(IAcceptorHalfBlocking.getTransformer())
def pluginWeb = new PluginWeb(hostname, portHttp, portWebSocket, new ImmutableArray(cgiRouters))
pluginWeb.httpServer.authenticator = new BasicAuthenticator("WebInterface") {

			@Override
			public boolean checkCredentials(String username, String password) {
				if (username.contains(":")) return false
				return "$username:$password" ==~ /[a-zA-Z0-9_]{1,16}:ho-tyo- tou4rou/
			}

		}
def pluginProcess = new PluginProcess() {

			@Override
			protected String[] getCommand() {
				["perl", "test.pl"] as String[]
			}

			@Override
			protected File getCurrentDirectory() {
				new File(".")
			}

		}

// コネクション宣言
pluginProcess.exportBus.addExporter hopperOutputBus.importBus.addImporter()
pluginWeb.exportBus.addExporter hopperInputBus.importBus.addImporter()
hopperInputBus.exportBus.addExporter pluginProcess.importBus.addImporter()
hopperInputBus.exportBus.addExporter hopperOutputBus.importBus.addImporter()
hopperOutputBus.exportBus.addExporter pluginWeb.importBus.addImporter()

// 開始
hopperOutputBus.start()
pluginWeb.start()
pluginProcess.start()

pluginProcess.up()
