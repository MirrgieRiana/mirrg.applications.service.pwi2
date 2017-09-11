package mirrg.applications.service.pwi2.web;

import java.util.ArrayList;
import java.util.Optional;

public class ConfigureWeb
{

	public int requestBufferSize;
	public int responseBufferSize;
	public int timeoutMs;

	public String host;
	public int port;
	public int backlog;

	public ArrayList<ConfigureCGIRouting> cgiRoutings = new ArrayList<>();

	public Optional<IAuthenticator> authenticator;

}
