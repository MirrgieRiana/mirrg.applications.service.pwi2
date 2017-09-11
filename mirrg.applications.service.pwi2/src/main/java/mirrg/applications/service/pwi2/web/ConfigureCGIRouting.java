package mirrg.applications.service.pwi2.web;

import java.io.File;
import java.util.ArrayList;

public class ConfigureCGIRouting
{

	public String serverName;
	public File documentRoot;
	public ArrayList<String> indexes = new ArrayList<>();
	public ArrayList<ConfigureCGIApplication> cgiApplications = new ArrayList<>();

}
