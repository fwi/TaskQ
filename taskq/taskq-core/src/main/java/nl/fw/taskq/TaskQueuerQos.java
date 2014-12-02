package nl.fw.taskq;

import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskQueuerQos<TASK_TYPE, TASK_QOS_TYPE> implements ITaskQueuerQos<TASK_TYPE, TASK_QOS_TYPE> {

	private static final Logger log = LoggerFactory.getLogger(TaskQueuerQos.class);

	private final ITaskQProgressByQosKey<TASK_QOS_TYPE> progressByQosKey;
	private final SyncListMap<TASK_QOS_TYPE, TASK_TYPE> tasksByKey = new SyncListMap<TASK_QOS_TYPE, TASK_TYPE>();
	
	/** Need a fair semaphore so that add does not block poll forever and vice versa. */
	private final Semaphore updateLock = new Semaphore(1, true);
	
	public TaskQueuerQos(ITaskQProgressByQosKey<TASK_QOS_TYPE> progressByQosKey) {
		this.progressByQosKey = progressByQosKey;
	}

	@Override
	public <QTT extends TASK_TYPE> boolean add(QTT task) {
		return add(task, null);
	}

	/**
	 * Add-method is synchronized with poll-method since both can add and remove Qos-keys
	 * and the poll-method requires the amount of Qos-keys to be constant during the poll-operation.
	 */
	@Override
	public <QTT extends TASK_TYPE> boolean add(QTT task, TASK_QOS_TYPE qosKey) {
		
		if (qosKey == null) {
			return tasksByKey.add(qosKey, task);
		}
		boolean havePermit = false;
		try {
			updateLock.acquire();
			havePermit = true;
			tasksByKey.add(qosKey, task);
		} catch (Exception e) {
			log.error("Could not add task [" + task + "] to queue.", e);
		} finally {
			if (havePermit) {
				updateLock.release();
			}
		}
		return true;
	}

	@Override
	public TASK_TYPE poll() {
		
		if (tasksByKey.getSize() < 1) {
			return null;
		}
		boolean havePermit = false;
		TASK_TYPE task = null;
		try {
			updateLock.acquire();
			havePermit = true;
			int maxKeys = tasksByKey.getSizeKeys();
			if (maxKeys < 1) {
				// fast track - no need to check for any in-progress counts.
				return tasksByKey.remove(tasksByKey.nextKey());
			}
			int maxPerKey = progressByQosKey.getMaxInProgressPerQosKey();
			if (maxPerKey < 1) {
				maxPerKey = progressByQosKey.getMaxInProgress();
			}
			int keyNumber = 0;
			while (task == null && keyNumber < maxKeys) {
				TASK_QOS_TYPE key = tasksByKey.nextKey();
				if (maxPerKey > progressByQosKey.getInProgress(key)) {
					task = tasksByKey.remove(key); 
				}
				keyNumber++;
			}
			if (task == null) {
				task = tasksByKey.remove(tasksByKey.nextKey());
			}
		} catch (Exception e) {
			log.warn("Could not remove a task from queue.", e);
		} finally {
			if (havePermit) {
				updateLock.release();
			}
		}
		return task;
	}

	@Override
	public int getSize() {
		return tasksByKey.getSize();
	}

	@Override
	public int getSizeKeys() {
		return tasksByKey.getSizeKeys();
	}
	
	protected Semaphore getUpdateLock() {
		return updateLock;
	}

	protected SyncListMap<TASK_QOS_TYPE, TASK_TYPE> getTasksByQosKey() {
		return tasksByKey;
	}

}
