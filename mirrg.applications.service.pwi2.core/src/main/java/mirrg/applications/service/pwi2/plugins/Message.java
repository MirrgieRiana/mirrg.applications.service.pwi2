package mirrg.applications.service.pwi2.plugins;

import java.time.Instant;

import net.arnx.jsonic.JSONHint;

public class Message
{

	@JSONHint(format = "yyyy/MM/dd HH:mm:ss.SSS")
	public final Instant time;
	public final Source source;
	public final String position;
	public final String text;

	public Message(Instant time, Source source, String position, String text)
	{
		this.time = time;
		this.source = source;
		this.position = position;
		this.text = text;
	}

	public Message(Source source, String position, String text)
	{
		this(Instant.now(), source, position, text);
	}

}
