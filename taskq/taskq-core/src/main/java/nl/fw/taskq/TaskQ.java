package nl.fw.taskq;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskQ<TASK_TYPE> implements ITaskQ<TASK_TYPE> {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(TaskQ.class);

	private ITaskQueuer<TASK_TYPE> taskQueuer;
	private ITaskRunnerFactory<TASK_TYPE> taskRunnerFactory;
	private ITaskExec taskExec;
	private final AtomicInteger inprogress = new AtomicInteger();
	private int maxInProgress = 4;
	private volatile boolean paused;
	private ITaskDoneListener<TASK_TYPE> taskDoneListener;
	
	@Override
	public ITaskQueuer<TASK_TYPE> getTaskQueuer() {
		return taskQueuer;
	}
	
	public void setTaskQueuer(ITaskQueuer<TASK_TYPE> taskQueuer) {
		this.taskQueuer = taskQueuer;
	}

	@Override
	public <QTT extends TASK_TYPE> boolean enqueue(QTT task) {
		
		boolean added = false;
		if (getTaskQueuer().add(task)) {
			added = true;
			triggerTaskRun();
		}
		return added;
	}
	
	protected void triggerTaskRun() {
		
		if (getTaskExec() != null 
				&& !isPaused() 
				&& getTaskQueuer().getSize() > 0) {
			getTaskExec().triggerCheckForTasks();
		}
	}
	
	@Override
	public TASK_TYPE dequeue() {
		return getTaskQueuer().poll();
	}

	@Override
	public ITaskRunnerFactory<TASK_TYPE> getTaskRunnerfactory() {
		return taskRunnerFactory;
	}

	public void setTaskRunnerFactory(ITaskRunnerFactory<TASK_TYPE> taskRunnerFactory) {
		this.taskRunnerFactory = taskRunnerFactory;
	}

	@Override
	public ITaskExec getTaskExec() {
		return taskExec;
	}

	@Override
	public void setTaskExec(ITaskExec taskExec) {

		this.taskExec = taskExec;
		triggerTaskRun();
	}

	@Override
	public int getInProgressCount() {
		return inprogress.get();
	}

	@Override
	public <QTT extends TASK_TYPE> int addInProgress(QTT task) {
		return inprogress.incrementAndGet();
	}

	@Override
	public <QTT extends TASK_TYPE> int removeInProgress(QTT task) {
		
		int remaining = inprogress.decrementAndGet(); 
		triggerTaskRun();
		if (getTaskDoneListener() != null) {
			getTaskDoneListener().taskDone(this, task);
		}
		return remaining;
	}
	
	@Override
	public boolean isMaxInProgress() {
		return (isPaused() || getInProgressCount() >= getMaxInProgress()); 
	}

	public int getMaxInProgress() {
		return maxInProgress;
	}

	public void setMaxInProgress(int maxInProgress) {
		this.maxInProgress = maxInProgress;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		
		this.paused = paused;
		if (!paused) {
			triggerTaskRun();
		}
	}

	@Override
	public ITaskDoneListener<TASK_TYPE> getTaskDoneListener() {
		return taskDoneListener;
	}

	@Override
	public void setTaskDoneListener(ITaskDoneListener<TASK_TYPE> taskDoneListener) {
		this.taskDoneListener = (ITaskDoneListener<TASK_TYPE>) taskDoneListener;
	}

}
