package mirrg.applications.service.pwi2.web;

public class ConfigureCGIApplication
{

	public String extension;
	public String[] commandFormat;

	public ConfigureCGIApplication(String extension, String[] commandFormat)
	{
		this.extension = extension;
		this.commandFormat = commandFormat;
	}

}
