package nl.fw.taskq;

public interface ITaskRunnerFactory<TASK_TYPE> {
	
	<QTT extends TASK_TYPE> ITaskRunner<TASK_TYPE> getTaskRunner(QTT task);

}
