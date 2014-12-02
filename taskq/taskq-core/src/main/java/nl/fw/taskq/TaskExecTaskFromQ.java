package nl.fw.taskq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecTaskFromQ<TASK_TYPE> implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(TaskExecTaskFromQ.class);
	
	private final ITaskQProgress<TASK_TYPE> taskQ;
	private final TASK_TYPE task;
	private final ITaskRunner<TASK_TYPE> taskRunner;
	
	public TaskExecTaskFromQ(ITaskQ<TASK_TYPE> taskQ, TASK_TYPE task) {
		this.taskQ = taskQ;
		this.task = task;
		this.taskRunner = taskQ.getTaskRunnerfactory().getTaskRunner(task);
	}
	
	@Override
	public void run() {
		
		boolean success = false;
		Exception error = null;
		try {
			if (log.isTraceEnabled()) {
				log.trace("Executing task " + task);
			}
			try {
				success = taskRunner.onTask(task);
			} catch (Exception e) {
				error = e;
				log.error("Task execution for task [" + task + "] in task queue [" + taskQ + "] failed unexpectedly.", e);
			}
			if (!success) {
				try {
					taskRunner.onTaskFailure(task, error);
				} catch (Exception e) {
					log.error("Failed to handle failed task [" + task + "] in task queue [" + taskQ + "].", e);
				}
			}
		} finally {
			taskQ.removeInProgress(task);
		}
	}

}
