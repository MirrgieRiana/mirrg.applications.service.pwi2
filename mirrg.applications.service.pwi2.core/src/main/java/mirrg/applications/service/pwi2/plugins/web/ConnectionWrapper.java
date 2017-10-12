package mirrg.applications.service.pwi2.plugins.web;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.java_websocket.WebSocket;

import mirrg.applications.service.pwi2.core.ITerminal;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorHalfBlocking;
import mirrg.applications.service.pwi2.core.containers.BufferedHopper;
import mirrg.applications.service.pwi2.plugins.IndexedMessage;
import net.arnx.jsonic.JSON;

public class ConnectionWrapper
{

	private BufferedHopper<IndexedMessage[]> hopper;
	private ITerminal<IAcceptorHalfBlocking<IndexedMessage[]>> importer;

	public ConnectionWrapper(WebSocket connection)
	{

		// コンテナ
		hopper = new BufferedHopper<>(100);

		// コネクション
		importer = hopper.addImporter();
		hopper.addExporter(() -> ms -> {
			connection.send(getStringFromMessages(ms));
		});

	}

	public void start()
	{
		hopper.start();
	}

	public void stop()
	{
		importer.close();
	}

	public void accept(IndexedMessage... indexedMessages) throws InterruptedException
	{
		importer.getAcceptor().accept(indexedMessages);
	}

	public static String getStringFromMessages(IndexedMessage... indexedMessages)
	{
		return Stream.of(indexedMessages)
			.map(m -> {
				String string = "MessageAdded " + JSON.encode(m);
				return string.length() + ":" + string;
			})
			.collect(Collectors.joining());
	}

}
