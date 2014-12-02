package nl.fw.taskq;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class TestTaskQWithQos {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Test
	public void testTaskQos() throws Exception {
		
		// Check execution of one task with null Qos-key
		TaskQWithQosCountDown taskQ = new TaskQWithQosCountDown();
		CountDownTask task = new CountDownTask();
		taskQ.setMaxInProgress(1); // For easier testing.
		taskQ.enqueue(task);
		TaskExec<Runnable> texec = new TaskExec<Runnable>();
		texec.add(taskQ);
		texec.start();
		boolean testOk = false;
		try {
			assertTrue("Executing first task.", task.running.await(1, TimeUnit.SECONDS));
			assertEquals("One qos task in progress.", 1, taskQ.getInProgress(null));
			assertEquals("No qos tasks remaining.", 0, taskQ.getTaskQueuer().getSize());
			assertEquals("No qos keys used.", 0, taskQ.getTaskQueuer().getSizeKeys());
			task.finish.countDown();
			assertTrue("Removed first task.", task.done.await(1, TimeUnit.SECONDS));
			assertEquals("No qos task in progress.", 0, taskQ.getInProgress(null));
			testOk = true;
		} finally {
			if (!testOk) {
				texec.close();
			}
		}
		
		// Check alternating between Qos keys.
		taskQ.setPaused(true);
		List<CountDownTask> tlist = new ArrayList<CountDownTask>();
		for (int i = 0; i < 10; i++) {
			task = new CountDownTask();
			task.id = i;
			if (i < 3) {
				task.qosKey = null;
			} else if (i < 6) {
				task.qosKey = "one";
			} else {
				task.qosKey = "two";
			}
			tlist.add(task);
			taskQ.enqueue(task);
		}
		// Expected order of task execution.
		List<Integer> torder = new ArrayList<Integer>();
		torder.add(0);
		torder.add(3);
		torder.add(6);
		torder.add(1);
		torder.add(4);
		torder.add(7);
		torder.add(2);
		torder.add(5);
		torder.add(8);
		torder.add(9);
		
		taskQ.setMaxInProgressPerQosKey(1);
		taskQ.setPaused(false);
		testOk = false;
		try {
			for (int i = 0; i < torder.size(); i++) {
				int taskNumber = torder.get(i);
				task = tlist.get(taskNumber);
				//System.out.println("Waiting for " + i + " / " + taskNumber + " / " + task.id + ", remaining: " + taskQ.getTaskQueuer().getSize());
				assertTrue("Executing qos task " + taskNumber, task.running.await(1, TimeUnit.SECONDS));
				task.finish.countDown();
			}
			testOk = true;
		} finally {
			if (!testOk) {
				texec.close();
			}
		}
		texec.close();
	}
	
}
