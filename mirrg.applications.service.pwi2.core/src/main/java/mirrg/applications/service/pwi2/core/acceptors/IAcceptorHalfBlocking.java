package mirrg.applications.service.pwi2.core.acceptors;

import java.util.List;
import java.util.function.Supplier;

import mirrg.applications.service.pwi2.core.IAcceptorTransformer;

/**
 * {@link #accept(Object)} メソッドが一定の速度に達するまではブロッキングを行わないことを表します。
 */
public interface IAcceptorHalfBlocking<T>
{

	public static <T> IAcceptorTransformer<IAcceptorHalfBlocking<T>> getTransformer()
	{
		return new IAcceptorTransformer<IAcceptorHalfBlocking<T>>() {

			@Override
			public IAcceptorHalfBlocking<T> doIf(Supplier<Boolean> sCondition, Supplier<IAcceptorHalfBlocking<T>> sAcceptor)
			{
				return t -> {
					if (sCondition.get()) {
						sAcceptor.get().accept(t);
					}
				};
			}

			@Override
			public IAcceptorHalfBlocking<T> doForEach(Supplier<List<IAcceptorHalfBlocking<T>>> sAcceptors)
			{
				return t -> {
					for (IAcceptorHalfBlocking<T> acceptor : sAcceptors.get()) {
						acceptor.accept(t);
					}
				};
			}

			@Override
			public IAcceptorHalfBlocking<T> doSynchronized(Supplier<Object> sLock, Supplier<IAcceptorHalfBlocking<T>> sAcceptor)
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
