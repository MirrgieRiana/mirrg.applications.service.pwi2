package mirrg.applications.service.pwi2.core.containers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

import mirrg.applications.service.pwi2.core.IExportBus;
import mirrg.applications.service.pwi2.core.IImportBus;
import mirrg.applications.service.pwi2.core.ITerminal;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorBlocking;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorHalfBlocking;

/**
 * 決められた容量の内部バッファを持ち、処理用スレッドがオブジェクトを処理するホッパーです。
 */
public class BufferedHopper<T> implements IHopper<T, IAcceptorHalfBlocking<T>, IAcceptorBlocking<T>>, IImportBus<IAcceptorHalfBlocking<T>>, IExportBus<IAcceptorBlocking<T>>
{

	private final Object lock = new Object();

	private volatile boolean closed = false;

	private final int bufferSize;
	private final ArrayDeque<T> queue = new ArrayDeque<>();

	private final HashSet<ITerminal<IAcceptorHalfBlocking<T>>> importers = new HashSet<>();
	private final ArrayList<ITerminal<IAcceptorBlocking<T>>> exporters = new ArrayList<>();
	private final Thread thread = new Thread(() -> {
		while (true) {
			synchronized (lock) {
				while (true) {
					if (closed) return;
					if (queue.size() > 0) break;
					try {
						lock.wait();
					} catch (InterruptedException e) {
						return;
					}
				}

				T t = queue.removeFirst();
				for (ITerminal<IAcceptorBlocking<T>> exporter : exporters) {
					try {
						exporter.getAcceptor().accept(t);
					} catch (InterruptedException e) {
						return;
					}
				}
				update();
			}
		}

	});

	public BufferedHopper(int bufferSize)
	{
		this.bufferSize = bufferSize;
	}

	@Override
	public void start()
	{
		synchronized (lock) {
			update();
		}
		if (!isClosed()) thread.start();
	}

	private void update()
	{
		if (!closed) {
			if (importers.size() <= 0 && queue.size() <= 0) {
				closed = true;
				for (ITerminal<IAcceptorBlocking<T>> exporter : exporters) {
					exporter.close();
				}
			}
		}
		lock.notifyAll();
	}

	@Override
	public void join() throws InterruptedException
	{
		synchronized (lock) {
			while (!isClosed()) {
				lock.wait();
			}
		}
	}

	@Override
	public boolean isClosed()
	{
		synchronized (lock) {
			return closed;
		}
	}

	@Override
	public IImportBus<IAcceptorHalfBlocking<T>> getImportBus()
	{
		return this;
	}

	@Override
	public IExportBus<IAcceptorBlocking<T>> getExportBus()
	{
		return this;
	}

	@Override
	public ITerminal<IAcceptorHalfBlocking<T>> addImporter()
	{
		return new ITerminal<IAcceptorHalfBlocking<T>>() {

			{
				synchronized (lock) {
					importers.add(this);
				}
			}

			@Override
			public IAcceptorHalfBlocking<T> getAcceptor()
			{
				return t -> {
					synchronized (lock) {
						while (queue.size() >= bufferSize) {
							lock.wait();
						}
						queue.addLast(t);
						update();
					}
				};
			}

			@Override
			public void close()
			{
				synchronized (lock) {
					importers.remove(this);
					update();
				}
			}

		};
	}

	@Override
	public void addExporter(ITerminal<IAcceptorBlocking<T>> exporter)
	{
		synchronized (lock) {
			exporters.add(exporter);
		}
	}

}
