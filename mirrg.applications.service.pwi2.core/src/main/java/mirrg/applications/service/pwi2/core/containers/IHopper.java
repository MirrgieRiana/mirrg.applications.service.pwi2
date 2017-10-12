package mirrg.applications.service.pwi2.core.containers;

import mirrg.applications.service.pwi2.core.IExportBus;
import mirrg.applications.service.pwi2.core.IImportBus;

/**
 * <p>
 * ホッパーはインポートバスとエクスポートバスを一つずつもち、
 * 特定のオブジェクト {@link T} を自動的にエクスポーターに受け渡すことができるコンテナです。
 * </p>
 * <p>
 * {@link IHopper#start()} は必ず1回だけ呼ばれ、
 * 全てのインポートバス及びエクスポートバスはそれより前に登録を完了していなければなりません。
 * </p>
 * <p>
 * 全てのインポーターが閉じられたとき、全てのエクスポーターは閉じられます。
 * </p>
 */
public interface IHopper<T, IA, EA>
{

	/**
	 * ホッパーのオートメーション動作を開始します。
	 */
	public void start();

	/**
	 * このホッパーが閉じられるまで待機します。
	 */
	public void join() throws InterruptedException;

	public boolean isClosed();

	public IImportBus<IA> getImportBus();

	public IExportBus<EA> getExportBus();

}
