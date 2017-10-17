package mirrg.applications.service.pwi2.core.plugins.web;

import java.net.InetSocketAddress;

public interface IWebCommandHandler
{

	public void accept(InetSocketAddress remoteSocketAddress, String command, String argument);

}
