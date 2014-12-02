package nl.fw.taskq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskQCountDown extends RunnableTaskQ<CountDownTask> {

	private static final Logger log = LoggerFactory.getLogger(TaskQCountDown.class);

	@Override 
	public int removeInProgress(CountDownTask task) {
	
		int i = super.removeInProgress(task);
		task.done.countDown();
		log.debug("Task " + task.id + " done.");
		return i;
	}

}
