package mirrg.applications.service.pwi2.plugins.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import mirrg.applications.service.pwi2.core.IExportBus;
import mirrg.applications.service.pwi2.core.IImportBus;
import mirrg.applications.service.pwi2.core.ITerminal;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorHalfBlocking;
import mirrg.applications.service.pwi2.core.containers.BufferedHopper;
import mirrg.applications.service.pwi2.core.containers.Hopper;
import mirrg.applications.service.pwi2.plugins.Message;
import mirrg.applications.service.pwi2.plugins.Source;

public abstract class PluginProcess
{

	private Process process;
	private PrintStream out;

	public PluginProcess()
	{
		initHoppers();
	}

	protected abstract String[] getCommand();

	protected abstract File getCurrentDirectory();

	public void up() throws IOException
	{
		String[] command = getCommand();
		String command2 = String.join(" ", command);

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(getCurrentDirectory());
		process = processBuilder.start();

		new Thread(() -> {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				int r = 1;
				while (true) {
					String line;
					try {
						line = in.readLine();
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					if (line == null) break;
					try {
						exporter.getAcceptor().accept(new Message(
							new Source("STDOUT", "#000000", command2),
							"R" + r,
							line));
					} catch (InterruptedException e) {
						break;
					}
					r++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();

		out = new PrintStream(process.getOutputStream(), true);

	}

	public void down()
	{
		process.destroy();
	}

	//

	private Object lock = new Object();

	private BufferedHopper<Message> hopperIn;
	private Hopper<Message, IAcceptorHalfBlocking<Message>> hopperOut;

	private ITerminal<IAcceptorHalfBlocking<Message>> exporter;

	private void initHoppers()
	{

		// コンテナ
		hopperIn = new BufferedHopper<>(20);
		hopperOut = new Hopper<>(IAcceptorHalfBlocking.getTransformer());

		// コネクション
		hopperIn.addExporter(() -> m -> {
			synchronized (lock) {
				out.println(m.text);
			}
		});
		exporter = hopperOut.addImporter();

	}

	public void start() throws IOException
	{
		hopperIn.start();
		hopperOut.start();
		System.err.println("Process Start: `" + String.join(" ", getCommand()) + "`");
	}

	public void stop() throws IOException
	{
		exporter.close();
	}

	public IImportBus<IAcceptorHalfBlocking<Message>> getImportBus()
	{
		return hopperIn.getImportBus();
	}

	public IExportBus<IAcceptorHalfBlocking<Message>> getExportBus()
	{
		return hopperOut.getExportBus();
	}

}
