package mirrg.applications.service.pwi2.core.plugins.process;

import java.io.File;
import java.io.Reader;

import org.apache.commons.logging.LogFactory;

import mirrg.applications.service.pwi2.core.Message;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.TerminalClosedException;
import mirrg.lithium.objectduct.inventories.Hopper;
import mirrg.lithium.objectduct.inventories.Objectduct;
import mirrg.lithium.objectduct.logging.LoggerLater;

/**
 * {@link Reader} を占有し、入力文字列を行ごとに切り出して出力するインベントリです。
 */
public abstract class PluginProcess extends Objectduct
{

	public static final LoggerLater LOG = new LoggerLater(LogFactory.getLog(PluginProcess.class));

	//

	private Object lock = new Object();
	private ProcessSession processSession;

	protected abstract String[] getCommand();

	protected abstract File getCurrentDirectory();

	protected abstract int getMaxLength();

	//

	private Hopper<String> hopperIn;
	private Hopper<Message> hopperOut;

	public Terminal<String> getImporter()
	{
		return hopperIn.getImporter();
	}

	public void setExporter(Terminal<Message> exporter)
	{
		hopperOut.setExporter(exporter);
	}

	@Override
	protected void initInventories()
	{
		add(hopperIn = new Hopper<>(100));
		add(hopperOut = new Hopper<>(100));
	}

	@Override
	protected void initConnections()
	{
		hopperIn.setExporter(new Terminal<String>() {

			@Override
			protected void acceptImpl(String t) throws InterruptedException, TerminalClosedException
			{
				synchronized (lock) {
					if (processSession != null) {
						processSession.getImporter().accept(t);
						return;
					}
				}
				LOG.warn(() -> "This input was ignored because no process is running: " + t);
			}

			@Override
			protected void closeImpl()
			{
				LOG.info(() -> "Input closed.");
			}

		});
	}

	public boolean isRunning()
	{
		synchronized (lock) {
			return processSession != null;
		}
	}

	public void up() throws Exception
	{
		synchronized (lock) {
			if (processSession != null) {
				LOG.warn(() -> "Process is already running!");
				return;
			}

			LOG.info(() -> "Process Start: `" + String.join(" ", getCommand()) + "`");
			LOG.info(() -> "Current Directory: '" + getCurrentDirectory() + "'");

			processSession = new ProcessSession(getCommand(), getCurrentDirectory(), getMaxLength());
			processSession.init();
			processSession.setExporter(new Terminal<Message>() {

				@Override
				protected void closeImpl()
				{
					LOG.info(() -> "Process finished.");
					synchronized (lock) {
						processSession = null;
					}
				}

				@Override
				protected void acceptImpl(Message t) throws InterruptedException, TerminalClosedException
				{
					hopperOut.getImporter().accept(t);
				}

			});
			processSession.start();
		}
	}

	public void down()
	{
		synchronized (lock) {
			if (processSession == null) {
				LOG.warn(() -> "No process is running!");
				return;
			}
			processSession.destroy();
		}
	}

}
