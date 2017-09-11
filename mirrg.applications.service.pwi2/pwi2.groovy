import mirrg.applications.service.pwi2.web.*;

configure.web.requestBufferSize = 1000000
configure.web.responseBufferSize = 1000000
configure.web.timeoutMs = 5000

configure.web.host = "0.0.0.0"
configure.web.port = 3030
configure.web.backlog = 10

cgiRouting = new ConfigureCGIRouting()
cgiRouting.serverName = "WebInterface"
cgiRouting.documentRoot = new File("http_home")
cgiRouting.indexes.add("index.html")
cgiRouting.indexes.add("index.pl")
cgiRouting.cgiApplications.add(new ConfigureCGIApplication(".pl", [ "perl", "%s" ] as String[]))
configure.web.cgiRoutings.add(cgiRouting);

// Do not authenticate
//configure.authenticator = Optional.empty()

// Example: user="abc123" password="ho-tyo- tou4rou"
configure.web.authenticator = Optional.of(new AuthenticatorRegex("[a-zA-Z0-9_]{1,16}\nho-tyo- tou4rou"))
