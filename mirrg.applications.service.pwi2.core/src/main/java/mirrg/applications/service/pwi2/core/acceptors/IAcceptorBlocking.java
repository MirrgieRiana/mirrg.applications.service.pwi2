package mirrg.applications.service.pwi2.core.acceptors;

import java.util.List;
import java.util.function.Supplier;

import mirrg.applications.service.pwi2.core.IAcceptorTransformer;

/**
 * {@link #accept()} メソッドがブロッキングを取り除くための制御を全く行わないことを表します。
 */
public interface IAcceptorBlocking<T>
{

	public static <T> IAcceptorTransformer<IAcceptorBlocking<T>> getTransformer()
	{
		return new IAcceptorTransformer<IAcceptorBlocking<T>>() {

			@Override
			public IAcceptorBlocking<T> doIf(Supplier<Boolean> sCondition, Supplier<IAcceptorBlocking<T>> sAcceptor)
			{
				return t -> {
					if (sCondition.get()) {
						sAcceptor.get().accept(t);
					}
				};
			}

			@Override
			public IAcceptorBlocking<T> doForEach(Supplier<List<IAcceptorBlocking<T>>> sAcceptors)
			{
				return t -> {
					for (IAcceptorBlocking<T> acceptor : sAcceptors.get()) {
						acceptor.accept(t);
					}
				};
			}

			@Override
			public IAcceptorBlocking<T> doSynchronized(Supplier<Object> sLock, Supplier<IAcceptorBlocking<T>> sAcceptor)
			{
				return t -> {
					synchronized (sLock.get()) {
						sAcceptor.get().accept(t);
					}
				};
			}

		};
	}

	public void accept(T t) throws InterruptedException;

}
