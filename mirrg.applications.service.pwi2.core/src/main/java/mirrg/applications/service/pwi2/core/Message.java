package mirrg.applications.service.pwi2.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import net.arnx.jsonic.JSONHint;

public class Message
{

	public final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss.SSS");

	@JSONHint(format = "yyyy/MM/dd HH:mm:ss.SSS")
	public final LocalDateTime time;
	public final Source source;
	public final Optional<Position> position;
	public final String text;

	public Message(LocalDateTime time, Source source, Optional<Position> position, String text)
	{
		this.time = time;
		this.source = source;
		this.position = position;
		this.text = text;
	}

	public Message(Source source, Position position, String text)
	{
		this(LocalDateTime.now(), source, Optional.of(position), text);
	}

	public Message(Source source, String text)
	{
		this(LocalDateTime.now(), source, Optional.empty(), text);
	}

	@Override
	public String toString()
	{
		return String.format("[%s] [%s]: %s",
			FORMATTER.format(time),
			source.name + "/" + source.additional + position.map(p -> "/R" + p.row + "C" + p.column + "").orElse(""),
			text);
	}

}
