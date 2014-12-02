package nl.fw.taskq;

import java.util.List;

public interface ITaskQChained<TASK_TYPE, TASK_ID_TYPE> extends ITaskQ<TASK_TYPE> {
	
	<QTT extends TASK_TYPE> TASK_ID_TYPE getTaskId(QTT task);

	<QTT extends TASK_TYPE> TASK_ID_TYPE getAfter(QTT task);

	<QTT extends TASK_TYPE> List<TASK_TYPE> removeAfter(QTT task);

}
