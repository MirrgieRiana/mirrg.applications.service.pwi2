package mirrg.applications.service.pwi2;

import org.apache.commons.logging.LogFactory;

import mirrg.lithium.objectduct.logging.LoggerLater;

public class AutoRestarter
{

	private static final LoggerLater LOG = new LoggerLater(LogFactory.getLog(AutoRestarter.class));

	private Object lock = new Object();
	private Thread thread = null;
	private Runnable runnable;

	public AutoRestarter(Runnable runnable)
	{
		this.runnable = runnable;
	}

	public void up()
	{
		synchronized (lock) {
			if (thread == null) {
				thread = new Thread(() -> {
					while (true) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							break;
						}
						try {
							runnable.run();
						} catch (RuntimeException e) {
							LOG.warn(() -> "", () -> e);
						}
					}
				});
				thread.start();
			}
		}
	}

	public void down()
	{
		synchronized (lock) {
			if (thread != null) {
				thread.interrupt();
				thread = null;
			}
		}
	}

}
