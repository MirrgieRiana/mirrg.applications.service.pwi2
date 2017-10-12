package mirrg.applications.service.pwi2.core.containers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

import mirrg.applications.service.pwi2.core.IAcceptorTransformer;
import mirrg.applications.service.pwi2.core.IExportBus;
import mirrg.applications.service.pwi2.core.IImportBus;
import mirrg.applications.service.pwi2.core.ITerminal;

/**
 * 内部バッファを持たず、入力されたオブジェクトをその場で処理するホッパーです。
 * このホッパーはオートメーションを行いません。
 */
public class Hopper<T, A> implements IHopper<T, A, A>, IImportBus<A>, IExportBus<A>
{

	private IAcceptorTransformer<A> acceptorTransformer;

	protected final Object lock = new Object();

	private volatile boolean closed = false;

	private final HashSet<ITerminal<A>> importers = new HashSet<>();
	protected final ArrayList<ITerminal<A>> exporters = new ArrayList<>();

	public Hopper(IAcceptorTransformer<A> acceptorTransformer)
	{
		this.acceptorTransformer = acceptorTransformer;
	}

	@Override
	public void start()
	{
		synchronized (lock) {
			update();
		}
	}

	private void update()
	{
		if (!closed) {
			if (importers.size() <= 0) {
				closed = true;
				for (ITerminal<A> exporter : exporters) {
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
	public IImportBus<A> getImportBus()
	{
		return this;
	}

	@Override
	public IExportBus<A> getExportBus()
	{
		return this;
	}

	@Override
	public ITerminal<A> addImporter()
	{
		return new ITerminal<A>() {

			{
				synchronized (lock) {
					importers.add(this);
				}
			}

			@Override
			public A getAcceptor()
			{
				return acceptorTransformer.doSynchronized(
					() -> lock,
					() -> acceptorTransformer.doForEach(
						() -> exporters.stream()
							.map(e -> e.getAcceptor())
							.collect(Collectors.toCollection(ArrayList::new))));
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
	public void addExporter(ITerminal<A> exporter)
	{
		synchronized (lock) {
			exporters.add(exporter);
		}
	}

}
