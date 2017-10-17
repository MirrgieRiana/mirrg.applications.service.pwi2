package mirrg.applications.service.pwi2.core.inventories;

import java.io.IOException;
import java.io.Reader;

import mirrg.applications.service.pwi2.core.Position;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.TerminalClosedException;
import mirrg.lithium.objectduct.inventories.ObjectductThreaded;
import mirrg.lithium.struct.Tuple;

public class ReaderImporter extends ObjectductThreaded<Tuple<String, Position>>
{

	private Reader in;
	private int maxLength;

	public ReaderImporter(Reader in, int maxLength)
	{
		this.in = in;
		this.maxLength = maxLength;
	}

	public ReaderImporter(Reader in)
	{
		this(in, Integer.MAX_VALUE);
	}

	//

	private Terminal<Tuple<String, Position>> exporter;

	public void setExporter(Terminal<Tuple<String, Position>> exporter)
	{
		this.exporter = exporter;
	}

	@Override
	protected void initInventories()
	{

	}

	@Override
	protected void initConnections()
	{

	}

	@Override
	protected void runImpl() throws InterruptedException
	{
		try {
			int lastCh = -1;
			StringBuilder string = new StringBuilder();
			int row = 1;
			int column = 1;
			while (true) {
				int ch;
				try {
					ch = in.read();
				} catch (IOException e) {
					if (string.length() > 0) {
						exporter.accept(new Tuple<>(
							string.toString(),
							new Position(row, column, string.length())));
					}
					break;
				}

				if (ch == -1) {
					if (string.length() > 0) {
						exporter.accept(new Tuple<>(
							string.toString(),
							new Position(row, column, string.length())));
					}
					break;
				}

				if (ch == '\r') {
					exporter.accept(new Tuple<>(
						string.toString(),
						new Position(row, column, string.length())));

					string.setLength(0);
					row++;
					column = 1;

				} else if (ch == '\n') {
					if (lastCh != '\r') {
						exporter.accept(new Tuple<>(
							string.toString(),
							new Position(row, column, string.length())));

						string.setLength(0);
						row++;
						column = 1;

					}
				} else {
					string.append((char) ch);
					if (string.length() >= maxLength) {
						exporter.accept(new Tuple<>(
							string.toString(),
							new Position(row, column, string.length())));

						string.setLength(0);
						column += maxLength;

					}
				}

				lastCh = ch;
			}
		} catch (TerminalClosedException e) {
			throw new RuntimeException(e);
		} finally {
			exporter.close();
		}
	}

}
