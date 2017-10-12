package mirrg.applications.service.pwi2.core.acceptors;

import java.util.List;
import java.util.function.Supplier;

import mirrg.applications.service.pwi2.core.IAcceptorTransformer;

/**
 * {@link #accept()} メソッドがブロッキングを決して行わないことを表します。
 */
public interface IAcceptorNonBlocking<T>
{

	public static <T> IAcceptorTransformer<IAcceptorNonBlocking<T>> getTransformer()
	{
		return new IAcceptorTransformer<IAcceptorNonBlocking<T>>() {

			@Override
			public IAcceptorNonBlocking<T> doIf(Supplier<Boolean> sCondition, Supplier<IAcceptorNonBlocking<T>> sAcceptor)
			{
				return t -> {
					if (sCondition.get()) {
						sAcceptor.get().accept(t);
					}
				};
			}

			@Override
			public IAcceptorNonBlocking<T> doForEach(Supplier<List<IAcceptorNonBlocking<T>>> sAcceptors)
			{
				return t -> {
					for (IAcceptorNonBlocking<T> acceptor : sAcceptors.get()) {
						acceptor.accept(t);
					}
				};
			}

			@Override
			public IAcceptorNonBlocking<T> doSynchronized(Supplier<Object> sLock, Supplier<IAcceptorNonBlocking<T>> sAcceptor)
			{
				return t -> {
					synchronized (sLock.get()) {
						sAcceptor.get().accept(t);
					}
				};
			}

		};
	}

	public void accept(T t);

}
