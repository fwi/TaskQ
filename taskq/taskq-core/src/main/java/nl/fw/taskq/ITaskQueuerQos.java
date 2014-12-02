package nl.fw.taskq;

public interface ITaskQueuerQos<TASK_TYPE, TASK_QOS_TYPE> extends ITaskQueuer<TASK_TYPE> {

	<QTT extends TASK_TYPE> boolean add(QTT task, TASK_QOS_TYPE qosKey);
	int getSizeKeys();
}
