package mirrg.applications.service.pwi2.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

import mirrg.applications.service.pwi2.core.ITerminal;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorBlocking;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorHalfBlocking;
import mirrg.applications.service.pwi2.core.acceptors.IAcceptorNonBlocking;
import mirrg.applications.service.pwi2.core.containers.BufferedHopper;
import mirrg.applications.service.pwi2.core.containers.Hopper;
import mirrg.applications.service.pwi2.core.containers.IHopper;

public class TestHopper
{

	@Test
	public void test1() throws InterruptedException
	{
		testHopper(() -> new Hopper<>(IAcceptorBlocking.<Integer> getTransformer()), c -> c::accept, ip -> t -> {
			try {
				ip.accept(t);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		testHopper(() -> new Hopper<>(IAcceptorHalfBlocking.<Integer> getTransformer()), c -> c::accept, ip -> t -> {
			try {
				ip.accept(t);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		testHopper(() -> new Hopper<>(IAcceptorNonBlocking.<Integer> getTransformer()), c -> c::accept, ip -> t -> {
			ip.accept(t);
		});
		for (int i = 0; i < 10; i++) {
			testHopper(() -> new BufferedHopper<>(10), c -> c::accept, ip -> t -> {
				try {
					ip.accept(t);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	public <IP, EP> void testHopper(
		Supplier<IHopper<Integer, IP, EP>> sHopper,
		Function<Consumer<Integer>, EP> fExportPusher,
		Function<IP, Consumer<Integer>> adder) throws InterruptedException
	{
		testHopper1(sHopper, fExportPusher, adder);
		testHopper2(sHopper, fExportPusher, adder);
	}

	public <IP, EP> void testHopper1(
		Supplier<IHopper<Integer, IP, EP>> sHopper,
		Function<Consumer<Integer>, EP> fExportPusher,
		Function<IP, Consumer<Integer>> adder) throws InterruptedException
	{
		// コンテナ宣言;
		IHopper<Integer, IP, EP> hopper = sHopper.get();
		ArrayList<Integer> array1 = new ArrayList<>();
		ArrayList<Integer> array2 = new ArrayList<>();
		ArrayList<Integer> array3 = new ArrayList<>();

		// コネクション宣言
		ITerminal<IP> importer1 = hopper.getImportBus().addImporter();
		ITerminal<IP> importer2 = hopper.getImportBus().addImporter();
		class A implements ITerminal<EP>
		{

			private ArrayList<Integer> array;

			public A(ArrayList<Integer> array)
			{
				this.array = array;
			}

			@Override
			public void close()
			{
				array.add(-1);
			}

			@Override
			public EP getAcceptor()
			{
				return fExportPusher.apply(array::add);
			}

		}
		hopper.getExportBus().addExporter(new A(array1));
		hopper.getExportBus().addExporter(new A(array2));
		hopper.getExportBus().addExporter(new A(array3));

		// 開始
		hopper.start();

		// インポーターが行動する
		new Thread(() -> {
			adder.apply(importer1.getAcceptor()).accept(1);
			adder.apply(importer2.getAcceptor()).accept(10);
			adder.apply(importer2.getAcceptor()).accept(20);
			adder.apply(importer1.getAcceptor()).accept(2);
			adder.apply(importer2.getAcceptor()).accept(30);
			importer2.close();
			adder.apply(importer1.getAcceptor()).accept(3);
			importer1.close();
		}).start();

		// 待つ
		hopper.join();

		assertEquals("[1, 10, 20, 2, 30, 3, -1]", array1.toString());
		assertEquals("[1, 10, 20, 2, 30, 3, -1]", array2.toString());
		assertEquals("[1, 10, 20, 2, 30, 3, -1]", array3.toString());
	}

	public <IP, EP> void testHopper2(
		Supplier<IHopper<Integer, IP, EP>> sHopper,
		Function<Consumer<Integer>, EP> fExportPusher,
		Function<IP, Consumer<Integer>> adder) throws InterruptedException
	{
		// コンテナ宣言;
		ArrayList<Integer> in = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			in.add(i);
		}
		IHopper<Integer, IP, EP> hopper = sHopper.get();
		ArrayList<Integer> out = new ArrayList<>();

		// コネクション宣言
		ITerminal<IP> importer1 = hopper.getImportBus().addImporter();
		class A implements ITerminal<EP>
		{

			private ArrayList<Integer> array;

			public A(ArrayList<Integer> array)
			{
				this.array = array;
			}

			@Override
			public EP getAcceptor()
			{
				return fExportPusher.apply(array::add);
			}

		}
		hopper.getExportBus().addExporter(new A(out));

		// 開始
		hopper.start();

		// インポーターが行動する
		new Thread(() -> {
			in.forEach(i -> adder.apply(importer1.getAcceptor()).accept(i));
			importer1.close();
		}).start();

		// 待つ
		hopper.join();

		assertEquals(100, out.size());
		for (int i = 0; i < 100; i++) {
			assertEquals(i, (int) out.get(i));
		}
	}

}
