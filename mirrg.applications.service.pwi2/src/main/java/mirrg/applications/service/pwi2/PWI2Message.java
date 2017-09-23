package mirrg.applications.service.pwi2;

import java.time.Instant;

import net.arnx.jsonic.JSONHint;

public class PWI2Message
{

	@JSONHint(format = "yyyy/MM/dd HH:mm:ss.SSS")
	public final Instant time;
	public final PWI2Source source;
	public final String position;
	public final String text;

	public PWI2Message(Instant time, PWI2Source source, String position, String text)
	{
		this.time = time;
		this.source = source;
		this.position = position;
		this.text = text;
	}

}
