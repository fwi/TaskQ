package nl.fw.taskq.file;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import nl.fw.taskq.RunnableTaskQ;
import nl.fw.taskq.TaskExec;
import nl.fw.taskq.TaskQ;
import nl.fw.taskq.util.FileUtil;

import org.junit.Test;

public class TestTaskFile {

	@Test
	public void testTaskFile() throws Exception {
		
		FileTestTaskQGroup qgroup = new FileTestTaskQGroup();
		TaskQ<FileTask> qone = new RunnableTaskQ<FileTask>();
		TaskQ<FileTask> qtwo = new RunnableTaskQ<FileTask>();
		qgroup.addTaskQ("qone", qone);
		qgroup.addTaskQ("qtwo", qtwo);
		qgroup.setTaskExec(new TaskExec<FileTask>());
		qgroup.getTaskExec().start();
		boolean testOk = false;
		try {
			FileTask t1 = new FileTask();
			t1.qgroup = qgroup;
			qgroup.enqueue("qone", t1);
			assertTrue("Task 1 stored.", t1.storeDone.await(1, TimeUnit.SECONDS));
			t1 = qgroup.loadTask(t1.qname, t1.id);
			assertEquals("qtwo", t1.qname);
			qgroup.enqueue(t1.qname, t1);
			assertTrue("Task 1 done.", t1.deleteDone.await(1, TimeUnit.SECONDS));
			testOk = true;
		} finally {
			if (!testOk) {
				qgroup.getTaskExec().stop();
			}
			FileUtil.delTempFile(FileUtil.TmpDir + "qtwo");
		}
		qgroup.getTaskExec().stop();
	}
	
}
