package nl.fw.taskq;

public interface ITaskDoneListener<TASK_TYPE> {

	<QTT extends TASK_TYPE> void taskDone(TaskQ<TASK_TYPE> taskQ, QTT task);
}
