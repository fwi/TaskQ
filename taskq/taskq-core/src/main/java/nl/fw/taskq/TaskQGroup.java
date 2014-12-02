package nl.fw.taskq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskQGroup<TASK_TYPE, TASK_TYPE_ID> implements ITaskDoneListener<TASK_TYPE> {

	private static final Logger log = LoggerFactory.getLogger(TaskQGroup.class);

	private ConcurrentHashMap<String, ITaskQ<TASK_TYPE>> qByName = new ConcurrentHashMap<String, ITaskQ<TASK_TYPE>>();
	private int maxSize, maxSizePerQ;
	private TaskExec<TASK_TYPE> taskExec;
	private final AtomicInteger totalQueued = new AtomicInteger(); 
	private long maxBlockingMs = 50000;
	private final AtomicInteger waitingToQ = new AtomicInteger();
	private final Semaphore waitingToQLock = new Semaphore(0, true);
	private volatile boolean unlockAllDone;
	private final Semaphore allDoneLock = new Semaphore(0);
	
	public ITaskQ<TASK_TYPE> getTaskQ(String name) {
		return qByName.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public <QTT extends TASK_TYPE> ITaskQ<TASK_TYPE> addTaskQ(String name, ITaskQ<QTT> taskQ) {
		
		if (getTaskExec() != null) {
			getTaskExec().add(taskQ);
		}
		taskQ.setTaskDoneListener((ITaskDoneListener<QTT>) this);
		return qByName.put(name, (ITaskQ<TASK_TYPE>) taskQ);
	}

	public ITaskQ<TASK_TYPE> removeTaskQ(String name) {
		
		 ITaskQ<TASK_TYPE> taskQ = qByName.get(name);
		 if (taskQ != null) {
			 if (getTaskExec() != null) {
				 getTaskExec().remove(taskQ);
			 }
			 qByName.remove(name);
		 }
		return taskQ;
	}
	
	public List<String> getTaskQNames() {
		return new ArrayList<String>(qByName.keySet());
	}

	public <QTT extends TASK_TYPE> TASK_TYPE_ID getTaskId(QTT task) {
		return null;
	}
	
	public <QTT extends TASK_TYPE> boolean enqueue(String name, QTT task) {
		return enqueue(name, task, getMaxBlockingMs(), TimeUnit.MILLISECONDS);
	}

	public <QTT extends TASK_TYPE> boolean enqueue(String name, QTT task, long timeout, TimeUnit tunit) {
		
		boolean queued = false;
		boolean canQ = !isMaxSizeReached();
		if (!canQ) {
			waitingToQ.incrementAndGet();
			try {
				canQ = waitingToQLock.tryAcquire(timeout, tunit);
			} catch (Exception e) {
				log.debug("Waiting to queue task interrupted: " + e);
			} finally {
				waitingToQ.decrementAndGet();
			}
		}
		if (canQ) {
			queued = getTaskQ(name).enqueue(task);
		}
		if (queued) {
			totalQueued.incrementAndGet();
		}
		return queued;
	}
	
	@Override
	public <QTT extends TASK_TYPE> void taskDone(TaskQ<TASK_TYPE> taskQ, QTT task) {

		totalQueued.decrementAndGet();
		if (waitingToQ.get() > 0) {
			waitingToQLock.release();
		} else if (totalQueued.get() < 1 && unlockAllDone) {
			allDoneLock.release();
		}
	}
	
	public boolean awaitAllDone(long timeout, TimeUnit tunit) {
		
		unlockAllDone = true;
		if (totalQueued.get() > 0) {
			try {
				allDoneLock.tryAcquire(timeout, tunit);
			} catch (Exception ignored) {}
		} 
		return (totalQueued.get() < 1);
	}

	public boolean isMaxSizeReached() {
		return (getMaxSize() > 0 && totalQueued.get() > getSize());
	}
	
	public int getSize() {
		return totalQueued.get();
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public int getMaxSizePerQ() {
		return (maxSizePerQ == 0 ? getMaxSize() : maxSizePerQ);
	}

	public void setMaxSizePerQ(int maxSizePerQ) {
		this.maxSizePerQ = maxSizePerQ;
	}

	public TaskExec<TASK_TYPE> getTaskExec() {
		return taskExec;
	}

	public void setTaskExec(TaskExec<TASK_TYPE> taskExec) {
		
		this.taskExec = taskExec;
		for (ITaskQ<TASK_TYPE> taskq : qByName.values()) {
			taskExec.add(taskq);
		}
	}

	public long getMaxBlockingMs() {
		return maxBlockingMs;
	}

	public void setMaxBlockingMs(long maxBlockingMs) {
		this.maxBlockingMs = maxBlockingMs;
	}

}
