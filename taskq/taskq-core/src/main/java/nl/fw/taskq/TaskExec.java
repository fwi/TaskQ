package nl.fw.taskq;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExec<TASK_TYPE> implements ITaskExec, AutoCloseable {
	
	private static final Logger log = LoggerFactory.getLogger(TaskExec.class);
	
	private final BinarySemaphore tasksAvailableLock = new BinarySemaphore();
	private final Semaphore pausedLock = new Semaphore(0);
	//private final Object waitForTasksLock = new Object();
	//private final AtomicInteger tasksTriggered = new AtomicInteger();
	private ExecutorService executor;
	private boolean stopExecutorOnClose;
	private final Set<ITaskQ<TASK_TYPE>> taskQs = new CopyOnWriteArraySet<ITaskQ<TASK_TYPE>>();
	private volatile boolean stop, started, paused;
	
	public TaskExec() {
		executor = Executors.newCachedThreadPool();
		stopExecutorOnClose = true;
	}
	
	public TaskExec(ExecutorService executor) {
		this.executor = executor;
	}
	
	public ExecutorService getExecutor() {
		return executor;
	}
	
	public void setStopExecutorOnClose(boolean stopExecutorOnClose) {
		this.stopExecutorOnClose = stopExecutorOnClose;
	}
	
	public boolean isStopExecutorOnClose() {
		return stopExecutorOnClose;
	}
	
	@SuppressWarnings("unchecked")
	public <QTT extends TASK_TYPE> boolean add(ITaskQ<QTT> taskQ) {
		
		taskQ.setTaskExec(this);
		return taskQs.add((ITaskQ<TASK_TYPE>) taskQ);
	}
	
	public <QTT extends TASK_TYPE> boolean remove(ITaskQ<QTT> taskQ) {
		
		taskQ.setTaskExec(null);
		return taskQs.remove(taskQ);
	}
	
	public void start() {
		
		synchronized (this) {
			if (isStarted()) {
				log.warn("Task executor already started.");
			} else {
				started = true;
				executor.execute(new TaskExecMainLoop());
			}
		}
	}
	
	public boolean isStarted() {
		return started;
	}

	public void stop() {
		close();
	}
	
	public boolean isStopped() {
		return stop;
	}
	
	public ICloseExecutor getExecutorCloser() {
		return new CloseExecutor(log, 5000L, 2000L);
	}
	
	@Override
	public void close() {
		
		boolean closeDown = false;
		synchronized (this) {
			if (isStopped()) {
				log.info("Task executor already stopped or stopping.");
			} else {
				stop = true;
				closeDown = true;
				setPaused(false);
			}
		}
		if (!closeDown) {
			return;
		}
		triggerCheckForTasks();
		if (isStopExecutorOnClose()) {
			getExecutorCloser().close(getExecutor());
			if (!getExecutor().isTerminated()) {
				StringBuilder sb = new StringBuilder("Not all running tasks have finished.");
				for (ITaskQ<TASK_TYPE> taskQ : taskQs) {
					if (taskQ.getInProgressCount() > 0) {
						sb.append("\n\t").append(taskQ.getInProgressCount()).append(" running tasks in ").append(taskQ.toString());
					}
				}
				log.warn(sb.toString());
			} else {
				log.debug("All tasks finished, task executor closed.");
			}
		}
	}
	
	@Override
	public void triggerCheckForTasks() {
		tasksAvailableLock.release();
	}
	
	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		
		if (this.paused != paused && !paused) {
			pausedLock.release();
		}
		this.paused = paused;
	}

	class TaskExecMainLoop implements Runnable {

		@Override
		public void run() {
			
			try {
				loop();
			} catch (Throwable t) {
				log.error("Task executor stopped.", t);
			}
		}
		
		private void loop() {
			
			while (!stop) {
				// Check if task execution is paused
				if (isPaused()) {
					log.debug("Pausing task execution.");
					try {
						pausedLock.acquire();
					} catch (Exception e) {
						if (!stop) {
							log.error("Waiting for unlock of pausing task execution interrupted.", e);
						}
					}
					if (stop) {
						break;
					}
					log.debug("Continuing task execution after pause.");
				}
				// Execute tasks in task-queues
				for (ITaskQ<TASK_TYPE> taskQ : taskQs) {
					int tasksStarted = 0;
					while (!taskQ.isMaxInProgress()) {
						try { 
							TASK_TYPE task = taskQ.dequeue();
							if (task == null) {
								break;
							}
							taskQ.addInProgress(task);
							getExecutor().execute(new TaskExecTaskFromQ<TASK_TYPE>(taskQ, task));
							tasksStarted++;
						} catch (Exception e) {
							log.error("Failed to execute tasks from queue [" + taskQ + "]", e);
						}
					}
					if (tasksStarted > 0 && log.isTraceEnabled()) {
						log.trace("Started " + tasksStarted + " task(s) from queue " + taskQ);
					}
				}
				// Wait for new tasks or room for tasks to execute 
				if (!tasksAvailableLock.tryAcquire()) {
					if (log.isTraceEnabled()) {
						log.trace("Waiting for new tasks.");
					}
					try {
						tasksAvailableLock.acquire();
					} catch (Exception e) {
						if (!stop) {
							log.error("Waiting for available tasks failed.", e);
						}
					}
				}
			} // while !stop
			log.debug("Task executor stopped.");
		} // method loop
	} // class TaskExecMainLoop
	
}
