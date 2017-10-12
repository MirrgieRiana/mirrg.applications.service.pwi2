package mirrg.applications.service.pwi2.core.containers;

import java.util.ArrayDeque;
import java.util.function.IntFunction;

import mirrg.applications.service.pwi2.core.IImportBus;
import mirrg.applications.service.pwi2.core.ITerminal;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorNonBlocking;

/**
 * 直近の決められた個数の入力オブジェクトを取得できるコンテナです。
 */
public class Cache<T> implements IImportBus<IAcceptorNonBlocking<T>>, IAcceptorNonBlocking<T>
{

	private final Object lock = new Object();

	private final int bufferSize;
	private final ArrayDeque<T> queue = new ArrayDeque<>();

	public Cache(int bufferSize)
	{
		this.bufferSize = bufferSize;
	}

	public IImportBus<IAcceptorNonBlocking<T>> getImportBus()
	{
		return this;
	}

	@Override
	public ITerminal<IAcceptorNonBlocking<T>> addImporter()
	{
		return new ITerminal<IAcceptorNonBlocking<T>>() {

			@Override
			public IAcceptorNonBlocking<T> getAcceptor()
			{
				return Cache.this;
			}

			@Override
			public void close()
			{

			}

		};
	}

	@Override
	public void accept(T t)
	{
		synchronized (lock) {
			while (queue.size() >= bufferSize) {
				queue.removeFirst();
			}
			queue.addLast(t);
		}
	}

	/**
	 * @return 古いものから順番に並びます。
	 */
	public T[] toArray(IntFunction<T[]> function)
	{
		synchronized (lock) {
			return queue.toArray(function.apply(queue.size()));
		}
	}

}
