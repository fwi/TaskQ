package nl.fw.taskq;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A semaphore that releases at most one permit.
 * @author FWiers
 *
 */
public class BinarySemaphore {

	private final AtomicBoolean havePermit = new AtomicBoolean();
	private final Semaphore sync;
	
	public BinarySemaphore() {
		this(false);
	}

	public BinarySemaphore(boolean fair) {
		sync = new Semaphore(0, fair);
	}
	
	public boolean release() {
		
		boolean released = havePermit.compareAndSet(false, true);
		if (released) {
			sync.release();
		}
		return released;
	}
	
	public boolean tryAcquire() {
		
		boolean acquired = sync.tryAcquire();
		if (acquired) {
			havePermit.set(false);
		}
		return acquired;
	}

	public boolean tryAcquire(long timeout, TimeUnit tunit) throws InterruptedException {
		
		boolean acquired = sync.tryAcquire(timeout, tunit);
		if (acquired) {
			havePermit.set(false);
		}
		return acquired;
	}

	public void acquire() throws InterruptedException {
		
		sync.acquire();
		havePermit.set(false);
	}
	
	public void acquireUninterruptibly() {
		
		sync.acquireUninterruptibly();
		havePermit.set(false);
	}
	
}
