package nl.fw.taskq;

public interface ITaskQProgress<TASK_TYPE> {

	<QTT extends TASK_TYPE> int addInProgress(QTT task);
	<QTT extends TASK_TYPE> int removeInProgress(QTT task);
	boolean isMaxInProgress();
	
	int getInProgressCount();
	ITaskDoneListener<TASK_TYPE> getTaskDoneListener();
	void setTaskDoneListener(ITaskDoneListener<TASK_TYPE> taskDoneListener);

}
