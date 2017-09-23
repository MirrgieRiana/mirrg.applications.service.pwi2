package mirrg.applications.service.pwi2;

public class PWI2IndexedMessage
{

	public final int index;
	public final PWI2Message message;

	public PWI2IndexedMessage(int index, PWI2Message message)
	{
		this.index = index;
		this.message = message;
	}

}
