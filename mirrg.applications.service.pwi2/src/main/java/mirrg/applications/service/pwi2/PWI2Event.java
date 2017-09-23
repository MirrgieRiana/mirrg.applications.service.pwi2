package mirrg.applications.service.pwi2;

public class PWI2Event
{

	public static class AddMessage extends PWI2Event
	{

		public final PWI2IndexedMessage message;

		public AddMessage(PWI2IndexedMessage message)
		{
			this.message = message;
		}

	}

}
