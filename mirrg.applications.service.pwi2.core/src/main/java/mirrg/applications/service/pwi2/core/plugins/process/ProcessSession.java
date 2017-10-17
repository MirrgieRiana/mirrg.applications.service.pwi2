package mirrg.applications.service.pwi2.core.plugins.process;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import mirrg.applications.service.pwi2.core.Message;
import mirrg.applications.service.pwi2.core.Position;
import mirrg.applications.service.pwi2.core.Source;
import mirrg.applications.service.pwi2.core.inventories.ReaderImporter;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.TerminalClosedException;
import mirrg.lithium.objectduct.inventories.ImportBus;
import mirrg.lithium.objectduct.inventories.Objectduct;
import mirrg.lithium.struct.Tuple;

public class ProcessSession extends Objectduct
{

	private String[] command;
	private File currentDirectory;
	private int maxLength;

	private Process process;
	private PrintStream out;

	private Object lock = new Object();

	public ProcessSession(String[] command, File currentDirectory, int maxLength)
	{
		this.command = command;
		this.currentDirectory = currentDirectory;
		this.maxLength = maxLength;
	}

	//

	private Terminal<String> importer;
	private Terminal<Message> exporterStdout;
	private Terminal<Message> exporterStderr;
	private ImportBus<Message> importBusExporter;

	public Terminal<String> getImporter()
	{
		return importer;
	}

	public void setExporter(Terminal<Message> exporter)
	{
		importBusExporter.setExporter(exporter);
	}

	@Override
	protected void initInventories() throws Exception
	{
		add(importBusExporter = new ImportBus<>());
	}

	@Override
	protected void initConnections() throws Exception
	{
		importer = new Terminal<String>() {

			@Override
			protected void acceptImpl(String t) throws InterruptedException
			{
				synchronized (lock) {
					out.println(t);
				}
			}

			@Override
			protected void closeImpl()
			{
				synchronized (lock) {
					out.close();
				}
			}

		};
		exporterStdout = importBusExporter.addImporter();
		exporterStderr = importBusExporter.addImporter();
	}

	//

	@Override
	public void start() throws Exception
	{
		synchronized (lock) {
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(currentDirectory);
			process = processBuilder.start();

			startReaderImporter(
				process.getInputStream(),
				exporterStdout,
				new Source("STDOUT", "#000000", String.join(" ", command)));
			startReaderImporter(
				process.getErrorStream(),
				exporterStderr,
				new Source("STDERR", "#ff0000", String.join(" ", command)));

			out = new PrintStream(process.getOutputStream(), true);
		}
	}

	private void startReaderImporter(InputStream in, Terminal<Message> exporter, Source source) throws Exception
	{
		ReaderImporter readerImporterStdout = new ReaderImporter(new InputStreamReader(in), maxLength);
		readerImporterStdout.init();
		readerImporterStdout.setExporter(new Terminal<Tuple<String, Position>>() {

			@Override
			protected void acceptImpl(Tuple<String, Position> t) throws InterruptedException, TerminalClosedException
			{
				exporter.accept(new Message(source, t.y, t.x));
			}

			@Override
			protected void closeImpl()
			{
				exporter.close();
			}

		});
		readerImporterStdout.start();
	}

	@Override
	public void stop()
	{
		super.stop();
		synchronized (lock) {
			process.destroy();
		}
	}

}
