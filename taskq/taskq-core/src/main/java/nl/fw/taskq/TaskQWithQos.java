package nl.fw.taskq;

public class TaskQWithQos<TASK_TYPE, TASK_QOS_TYPE> extends TaskQ<TASK_TYPE> 
		implements ITaskQWithQos<TASK_TYPE, TASK_QOS_TYPE>, ITaskQProgressByQosKey<TASK_QOS_TYPE> {

	private SyncCountMap<TASK_QOS_TYPE> progressByKey = new SyncCountMap<TASK_QOS_TYPE>();
	private int maxInProgressPerQosKey;
	
	@Override
	public <QTT extends TASK_TYPE> boolean enqueue(QTT task) {
		
		boolean added = false;
		if (getTaskQueuer().add(task, getTaskQosKey(task))) {
			added = true;
			triggerTaskRun();
		}
		return added;
	}
	
	@Override
	public TASK_TYPE dequeue() {
		
		TASK_TYPE task = super.dequeue();
		if (task != null) {
			 progressByKey.increment(getTaskQosKey(task));
		}
		return task;
	}

	@Override
	public <QTT extends TASK_TYPE> int removeInProgress(QTT task) {
		
		progressByKey.decrement(getTaskQosKey(task));
		return super.removeInProgress(task);
	}

	@Override
	public int getInProgress(TASK_QOS_TYPE qosKey) {
		return progressByKey.getCount(qosKey);
	}

	@Override
	public <QTT extends TASK_TYPE> TASK_QOS_TYPE getTaskQosKey(QTT task) {
		return null;
	}
	
	@Override
	public int getMaxInProgress() {
		return super.getMaxInProgress();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ITaskQueuerQos<TASK_TYPE, TASK_QOS_TYPE> getTaskQueuer() {
		return (ITaskQueuerQos<TASK_TYPE, TASK_QOS_TYPE>) super.getTaskQueuer();
	}
	
	public void setTaskQueuer(ITaskQueuerQos<TASK_TYPE, TASK_QOS_TYPE> taskQueuer) {
		super.setTaskQueuer(taskQueuer);
	}

	@Override
	public int getMaxInProgressPerQosKey() {
		return maxInProgressPerQosKey;
	}
	
	public void setMaxInProgressPerQosKey(int maxInProgressPerQosKey) {
		this.maxInProgressPerQosKey = maxInProgressPerQosKey;
	}

}
