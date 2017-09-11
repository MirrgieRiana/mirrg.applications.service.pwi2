package mirrg.applications.service.pwi2.web;

import java.util.regex.Pattern;

public class AuthenticatorRegex implements IAuthenticator
{

	private Pattern pattern;

	public AuthenticatorRegex(String regex)
	{
		this.pattern = Pattern.compile(regex);
	}

	@Override
	public boolean test(String username, String password)
	{
		if (username.indexOf("\n") != -1) return false;
		return pattern.matcher(username + "\n" + password).matches();
	}

}
