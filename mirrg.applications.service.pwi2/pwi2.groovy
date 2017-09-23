import java.net.InetSocketAddress

import com.sun.net.httpserver.BasicAuthenticator
import com.sun.net.httpserver.HttpContext
import com.sun.net.httpserver.HttpServer

import mirrg.applications.service.pwi2.*
import mirrg.lithium.cgi.CGIBufferPool
import mirrg.lithium.cgi.CGIServerSetting
import mirrg.lithium.cgi.ILogger
import mirrg.lithium.cgi.routing.CGIPattern
import mirrg.lithium.cgi.routing.CGIRouter
import mirrg.lithium.cgi.routing.HttpHandlerCGIRouting

def cgiBufferPool = new CGIBufferPool(1000000, 1000000, 10, 10000)
def hostname = "0.0.0.0"
def port = 3030
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
	def cgiRouter = new CGIRouter(new CGIServerSetting(port, "WebInterface", new File("http_home"), 5000, cgiBufferPool))
	cgiRouter.addIndex "index.html"
	cgiRouter.addIndex "index.pl"
	cgiRouter.addCGIPattern new CGIPattern(".pl", ["perl", "%s"] as String[], logger, 1000)
	cgiRouter
}()

def pwi2 = new PWI2(hostname, port, portWebSocket, 50000)

def httpServer = pwi2.getHttpServer(cgiRouters as CGIRouter[])
httpServer.authenticator = new BasicAuthenticator("WebInterface") {
			@Override
			public boolean checkCredentials(String username, String password) {
				if (username.contains(":")) return false
				return "$username\n$password" ==~ /[a-zA-Z0-9_]{1,16}\nho-tyo- tou4rou/
			}
		}
httpServer.server.start()
System.err.println("HTTP Server Start: http://$hostname:$port")

def webSocketServer = pwi2.getWebSocketServer()
webSocketServer.start()
System.err.println("WebSocket Server Start: http://$hostname:$portWebSocket")
