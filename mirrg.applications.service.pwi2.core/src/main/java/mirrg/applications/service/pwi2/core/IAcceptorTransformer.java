package mirrg.applications.service.pwi2.core;

import java.util.List;
import java.util.function.Supplier;

public interface IAcceptorTransformer<A>
{

	public A doIf(Supplier<Boolean> sCondition, Supplier<A> sAcceptor);

	public A doForEach(Supplier<List<A>> sAcceptors);

	public A doSynchronized(Supplier<Object> sLock, Supplier<A> sAcceptor);

}
