package mirrg.applications.service.pwi2.core;

public class IndexedMessage
{

	public final int index;
	public final Message message;

	public IndexedMessage(int index, Message message)
	{
		this.index = index;
		this.message = message;
	}

}
