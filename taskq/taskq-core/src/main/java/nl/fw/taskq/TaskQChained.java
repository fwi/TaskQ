package nl.fw.taskq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proof of concept, not suitable for production.
 * <br>Chaining tasks (or enforcing a certain order of execution) does not work well with task queues.
 * It is better to create new tasks from tasks that have to execute first. 
 * <br>If a task in the chain fails to execute, all remaining tasks need to be cleaned up.
 * Cleanup is a troublesome administration (e.g. prevent memory leaks) 
 * which becomes too complex in the multi-threaded task-queues environment. 
 * @author FWiers
 */
public class TaskQChained<TASK_TYPE, TASK_ID_TYPE> extends TaskQ<TASK_TYPE> implements ITaskQChained<TASK_TYPE, TASK_ID_TYPE> {

	//@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(TaskQ.class);

	private final Set<TASK_ID_TYPE> chainedTaskIds = Collections.newSetFromMap(new ConcurrentHashMap<TASK_ID_TYPE, Boolean>());
	protected final Object chainLock = new Object();
	
	/**
	 * Tasks that need to be executed after another task completed. 
	 */
	private final Map<TASK_ID_TYPE, List<TASK_TYPE>> chainedAfter = new HashMap<TASK_ID_TYPE, List<TASK_TYPE>>();

	@Override
	public <QTT extends TASK_TYPE> boolean enqueue(QTT task) {
		return enqueue(task, false);
	}
		
	public <QTT extends TASK_TYPE> boolean enqueue(QTT task, boolean requeued) {

		TASK_ID_TYPE taskId = getTaskId(task);
		if (taskId == null) {
			return super.enqueue(task);
		}
		if (!requeued && isInCache(taskId)) {
			throw new IllegalArgumentException("Task with ID [" + taskId + "] already in task queue [" + this.toString() + "].");
		}
		chainedTaskIds.add(taskId);
		TASK_ID_TYPE after = getAfter(task);
		boolean waitForUnchain = false;
		if (after != null) {
			if (after.equals(taskId)) {
				log.warn("Cannot chain a task after itself, task ID is [" + taskId + "]");
			} else {
				synchronized(chainLock) {
					if (isInCache(after)) {
						addToList(chainedAfter, after, task);
						waitForUnchain = true;
					}
				}
			}
		}
		return (waitForUnchain ? true : super.enqueue(task));
	}

	public boolean isInCache(TASK_ID_TYPE taskId) {
		return chainedTaskIds.contains(taskId);
	}
	
	public int getSizeAfter() {
		return chainedAfter.size();
	}
	
	protected <QTT extends TASK_TYPE> void unchain(QTT task) {
		
		TASK_ID_TYPE taskId = getTaskId(task);
		if (taskId == null) {
			return;
		}
		chainedTaskIds.remove(taskId);
		List<TASK_TYPE> afterTasks = null;
		synchronized(chainLock) {
			afterTasks = chainedAfter.remove(taskId);
		}
		if (afterTasks != null) {
			for (TASK_TYPE afterTask : afterTasks) {
				enqueue(afterTask, true);
			}
		}
	}

	@Override
	public <QTT extends TASK_TYPE> int removeInProgress(QTT task) {
		
		unchain(task);
		return super.removeInProgress(task);
	}

	@Override
	public <QTT extends TASK_TYPE> TASK_ID_TYPE getTaskId(QTT task) { 
		return null; 
	}

	@Override
	public <QTT extends TASK_TYPE> TASK_ID_TYPE getAfter(QTT task) { 
		return null; 
	}

	@Override
	public <QTT extends TASK_TYPE> List<TASK_TYPE> removeAfter(QTT task) { 
		
		List<TASK_TYPE> fromCache = new ArrayList<TASK_TYPE>();
		synchronized(chainLock) {
			fromCache.add(task);
			int start = 0;
			int end = 1;
			while (true) {
				for (int i = start; i < end; i++) {
					TASK_ID_TYPE taskId = getTaskId(fromCache.get(i));
					List<TASK_TYPE> ctaskFromCache = (taskId == null ? null : chainedAfter.remove(taskId));
					if (ctaskFromCache != null) {
						fromCache.addAll(ctaskFromCache);
					}
				}
				start = end;
				end = fromCache.size();
				if (start >= end) {
					break;
				}
			}
			fromCache.remove(0);
		}
		return fromCache; 
	}

	private <QTT extends TASK_TYPE> void addToList(Map<TASK_ID_TYPE, List<TASK_TYPE>> map, TASK_ID_TYPE key, QTT value) {
		
		List<TASK_TYPE> valueList = map.get(key);
		if (valueList == null) {
			valueList = new LinkedList<TASK_TYPE>();
			map.put(key, valueList);
		}
		valueList.add(value);
	}
}
