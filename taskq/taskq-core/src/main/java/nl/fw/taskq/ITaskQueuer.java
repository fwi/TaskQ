package nl.fw.taskq;

public interface ITaskQueuer<TASK_TYPE> {

	<QTT extends TASK_TYPE> boolean add(QTT task);
	
	TASK_TYPE poll();
	
	int getSize();
}
