package nl.fw.taskq;

public class RunnableTaskRunnerFactory<TASK_TYPE extends Runnable> implements ITaskRunnerFactory<TASK_TYPE> {

	private RunnableTaskRunner<TASK_TYPE> taskRunner = new RunnableTaskRunner<TASK_TYPE>();
	
	@Override
	public <QTT extends TASK_TYPE> ITaskRunner<TASK_TYPE> getTaskRunner(QTT task) {
		return taskRunner;
	}

	static class RunnableTaskRunner<TASK_TYPE extends Runnable> implements ITaskRunner<TASK_TYPE> {

		@Override
		public <QTT extends TASK_TYPE> boolean onTask(QTT task) {
			task.run();
			return true;
		}

		@Override
		public <QTT extends TASK_TYPE> void onTaskFailure(QTT task, Exception error) {
			// NO-OP
		}
		
	}
}
