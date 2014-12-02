package nl.fw.taskq;

public interface ITaskQ<TASK_TYPE> extends ITaskQProgress<TASK_TYPE> {
	
	ITaskQueuer<TASK_TYPE> getTaskQueuer();
	
	ITaskRunnerFactory<TASK_TYPE> getTaskRunnerfactory();

	void setTaskExec(ITaskExec taskExec);

	ITaskExec getTaskExec();

	<QTT extends TASK_TYPE> boolean enqueue(QTT task);

	TASK_TYPE dequeue();

}
