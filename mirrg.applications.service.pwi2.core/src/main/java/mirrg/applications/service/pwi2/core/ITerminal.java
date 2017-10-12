package mirrg.applications.service.pwi2.core;

public interface ITerminal<A>
{

	public A getAcceptor();

	public default void close()
	{

	}

}
