package mirrg.applications.service.pwi2.core.inventories;

import java.io.Reader;

import mirrg.applications.service.pwi2.core.Position;
import mirrg.lithium.objectduct.IInventory;
import mirrg.lithium.objectduct.Terminal;
import mirrg.lithium.objectduct.inventories.Hopper;
import mirrg.lithium.struct.Tuple;

public class DaemonReaderHopper implements IInventory
{

	private Reader in;

	public DaemonReaderHopper(Reader in)
	{
		this.in = in;
	}

	//

	private ReaderImporter readerImporter;
	private Hopper<Tuple<String, Position>> hopper;

	public ReaderImporter getReaderImporter()
	{
		return readerImporter;
	}

	public Hopper<Tuple<String, Position>> getHopper()
	{
		return hopper;
	}

	public void setExporter(Terminal<Tuple<String, Position>> exporter)
	{
		hopper.setExporter(exporter);
	}

	@Override
	public void init() throws Exception
	{
		readerImporter = new ReaderImporter(in);
		readerImporter.setDaemon(true);
		readerImporter.init();
		hopper = new Hopper<>(10);
		hopper.init();

		readerImporter.setExporter(hopper.getImporter());
	}

	@Override
	public void start() throws Exception
	{
		readerImporter.start();
		hopper.start();
	}

	@Override
	public void stop()
	{
		//readerImporter.stop();
		hopper.stop();
	}

	@Override
	public void join() throws InterruptedException
	{
		//readerImporter.join();
		hopper.join();
	}

}
