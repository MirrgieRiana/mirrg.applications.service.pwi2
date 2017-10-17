package mirrg.applications.service.pwi2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class FileIncrementer
{

	private File file;

	public FileIncrementer(File file)
	{
		this.file = file;
	}

	public int next() throws IOException
	{
		file.getAbsoluteFile().getParentFile().mkdirs();

		int value = file.exists() ? get() : 0;
		set(value + 1);
		return value;
	}

	private int get() throws IOException
	{
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			return Integer.parseInt(in.readLine().trim(), 10);
		}
	}

	private void set(int value) throws IOException
	{
		try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
			out.println(value);
		}
	}

}
