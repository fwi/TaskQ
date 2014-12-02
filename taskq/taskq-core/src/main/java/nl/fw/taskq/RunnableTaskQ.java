package nl.fw.taskq;

public class RunnableTaskQ<TASK_TYPE extends Runnable> extends TaskQ<TASK_TYPE> {
	
	public RunnableTaskQ() {
		setTaskRunnerFactory(new RunnableTaskRunnerFactory<TASK_TYPE>());
		setTaskQueuer(new TaskQueuerFifo<TASK_TYPE>());
	}

}
