package mirrg.applications.service.pwi2.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

public class AuthenticatorFile implements IAuthenticator
{

	private String[] entries;
	private MessageDigest messageDigest;

	public AuthenticatorFile(File file)
	{
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			entries = in.lines()
				.toArray(String[]::new);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean test(String username, String password)
	{
		String sha = DatatypeConverter.printHexBinary(messageDigest.digest(password.getBytes()));
		return Stream.of(entries)
			.anyMatch(e -> e.equals(username + ":" + sha));
	}

}
