package nl.fw.taskq;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskQueuerFifo<TASK_TYPE> implements ITaskQueuer<TASK_TYPE> {

	protected ConcurrentLinkedQueue<TASK_TYPE> q = new ConcurrentLinkedQueue<TASK_TYPE>(); 
	private final AtomicInteger qsize = new AtomicInteger();

	@Override
	public <QTT extends TASK_TYPE> boolean add(QTT task) {
		
		q.add(task);
		qsize.incrementAndGet();
		return true;
	}

	@Override
	public TASK_TYPE poll() {
		
		TASK_TYPE task = q.poll();
		if (task != null) {
			qsize.decrementAndGet();
		}
		return task;
	}
	
	@Override
	public int getSize() {
		return qsize.get();
	}

}
