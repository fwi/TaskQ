package nl.fw.taskq;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountDownTask implements Runnable {
	
	public static AtomicInteger ID = new AtomicInteger();

	private static final Logger log = LoggerFactory.getLogger(CountDownTask.class);

	public CountDownLatch running = new CountDownLatch(1);
	public CountDownLatch finish  = new CountDownLatch(1);
	public CountDownLatch done  = new CountDownLatch(1);
	public int id = ID.incrementAndGet();
	public int afterId;
	public String qosKey;
	
	@Override
	public void run() {
		
		try {
			running.countDown();
			log.debug("Task " + id + " running.");
			finish.await();
			log.debug("Task " + id + " finished.");
		} catch (Exception e) {
			throw new AssertionError("Task " + id  + "  was not executed.", e);
		}
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " " + id;
	}
}