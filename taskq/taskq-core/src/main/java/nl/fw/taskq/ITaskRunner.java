package nl.fw.taskq;

public interface ITaskRunner<TASK_TYPE> {

	<QTT extends TASK_TYPE> boolean onTask(QTT task);
	
	<QTT extends TASK_TYPE> void onTaskFailure(QTT task, Exception error);
}
