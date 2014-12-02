package nl.fw.taskq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskQWithQosCountDown extends RunnableTaskQWithQos<CountDownTask, String> {

	private static final Logger log = LoggerFactory.getLogger(TaskQWithQosCountDown.class);

	@Override
	public String getTaskQosKey(CountDownTask task) {
		return task.qosKey;
	}
	
	@Override
	public CountDownTask dequeue() {
		
		CountDownTask task = super.dequeue();
		if (task == null) {
			log.debug("Returning no task.");
		} else {
			log.debug("Returning task " + task.id + ", remaining: " + getTaskQueuer().getSize());
		}
		return task;
	}


	@Override 
	public int removeInProgress(CountDownTask task) {
	
		int i = super.removeInProgress(task);
		task.done.countDown();
		log.debug("Task " + task.id + " done.");
		return i;
	}

}
