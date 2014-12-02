package nl.fw.taskq;

public interface ITaskQWithQos<TASK_TYPE, TASK_QOS_TYPE> extends ITaskQ<TASK_TYPE> {

	<QTT extends TASK_TYPE> TASK_QOS_TYPE getTaskQosKey(QTT task);

}
