package mirrg.applications.service.pwi2.core.plugins.web;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.java_websocket.WebSocket;

import mirrg.applications.service.pwi2.core.IndexedMessage;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.inventories.Hopper;
import mirrg.lithium.objectduct.inventories.Objectduct;
import net.arnx.jsonic.JSON;

/**
 * WebSocketコネクションをラッピングし、
 * {@link IndexedMessage} の配列をノンブロッキングで受理し
 * WebSocketに渡すインベントリです。
 */
public class ObjectductWebSocket extends Objectduct
{

	private WebSocket connection;

	public ObjectductWebSocket(WebSocket connection)
	{
		this.connection = connection;
	}

	//

	private Hopper<IndexedMessage[]> hopper;

	private Terminal<IndexedMessage[]> importer;

	public Terminal<IndexedMessage[]> getImporter()
	{
		return importer;
	}

	@Override
	protected void initInventories()
	{
		add(hopper = new Hopper<>(100));
	}

	@Override
	protected void initConnections()
	{
		importer = hopper.getImporter();
		hopper.setExporter(new Terminal<IndexedMessage[]>() {

			@Override
			protected void acceptImpl(IndexedMessage[] t) throws InterruptedException
			{
				connection.send(getStringFromMessages(t));
			}

			@Override
			protected void closeImpl()
			{

			}

		});
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
