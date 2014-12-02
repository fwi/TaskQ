package nl.fw.taskq;

public interface ITaskQProgressByQosKey<TASK_QOS_TYPE> {

	int getInProgress(TASK_QOS_TYPE qoskey);
	int getMaxInProgress();
	int getMaxInProgressPerQosKey();
	
}
