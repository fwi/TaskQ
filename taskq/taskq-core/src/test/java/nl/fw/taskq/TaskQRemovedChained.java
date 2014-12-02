package nl.fw.taskq;

public class TaskQRemovedChained extends TaskQChained<CountDownTask, Integer> {

	public TaskQRemovedChained() {
		setTaskRunnerFactory(new RunnableTaskRunnerFactory<CountDownTask>());
		setTaskQueuer(new TaskQueuerFifo<CountDownTask>());
	}
	
	@Override
	public Integer getTaskId(CountDownTask task) { 
		return task.id; 
	}

	@Override
	public Integer getAfter(CountDownTask task) { 
		int afterId = task.afterId;
		return (afterId < 1 ? null : afterId); 
	}

	@Override 
	public int removeInProgress(CountDownTask task) {
	
		int i = super.removeInProgress(task);
		task.done.countDown();
		return i;
	}

}
