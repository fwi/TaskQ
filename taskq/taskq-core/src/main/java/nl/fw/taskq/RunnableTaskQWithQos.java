package nl.fw.taskq;

public class RunnableTaskQWithQos<TASK_TYPE extends Runnable, TASK_QOS_TYPE> extends TaskQWithQos<TASK_TYPE, TASK_QOS_TYPE> {
	
	public RunnableTaskQWithQos() {
		setTaskRunnerFactory(new RunnableTaskRunnerFactory<TASK_TYPE>());
		setTaskQueuer(new TaskQueuerQos<TASK_TYPE, TASK_QOS_TYPE>(this));
	}

}
