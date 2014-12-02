package nl.fw.taskq;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TestChainedTaskQ {

	@Test
	public void testChainedTaskQ() throws Exception {
		
		// Test unchained task.
		CountDownTask task = new CountDownTask();
		TaskQRemovedChained taskQ = new TaskQRemovedChained();
		int id = task.id;
		taskQ.enqueue(task);
		TaskExec<Runnable> texec = new TaskExec<Runnable>();
		texec.add(taskQ);
		texec.start();
		boolean testOk = false;
		try {
			assertTrue("Executing first non-chained task.", task.running.await(1, TimeUnit.SECONDS));
			assertEquals(1, taskQ.getInProgressCount());
			// check ID must be unique
			CountDownTask task2 = new CountDownTask();
			task2.id = id;
			try {
				taskQ.enqueue(task2);
				fail("Expected non-unique ID exception.");
			} catch (IllegalArgumentException ignored) {}
			
			task.finish.countDown();
			assertTrue("Removing first non-chained task.", task.done.await(1, TimeUnit.SECONDS));
			testOk = true;
		} finally {
			if (!testOk) {
				texec.close();
			}
		}
		
		// Test chained task
		task = new CountDownTask();
		CountDownTask task2 = new CountDownTask();
		task2.afterId = task.id;
		CountDownLatch tasksDone = new CountDownLatch(2);
		task.done = tasksDone;
		task2.done = tasksDone;
		taskQ.enqueue(task);
		taskQ.enqueue(task2);
		testOk = false;
		try {
			assertTrue("Executing first chained task.", task.running.await(1, TimeUnit.SECONDS));
			assertEquals(1, taskQ.getInProgressCount());
			// Chained task is in taskQ-cache not in queuer.
			assertEquals(1, taskQ.getSizeAfter());
			assertEquals(0, taskQ.getTaskQueuer().getSize());
	
			task.finish.countDown();
			assertTrue("Executing second chained task.", task2.running.await(1, TimeUnit.SECONDS));
			task2.finish.countDown();
			assertTrue("Second chained task finished.", tasksDone.await(1, TimeUnit.SECONDS));
			assertEquals("Cache should be empty.", 0, taskQ.getSizeAfter());
			assertEquals("No more tasks.", 0, taskQ.getTaskQueuer().getSize());
			
			testOk = true;
		} finally {
			if (!testOk) {
				texec.close();
			}
		}

		// Check removal of chained tasks from chained-tasks cache.
		task = new CountDownTask();
		task2 = new CountDownTask();
		task2.afterId = task.id;
		CountDownTask task3 = new CountDownTask();
		task3.afterId = task2.id;
		taskQ.enqueue(task);
		taskQ.enqueue(task2);
		taskQ.enqueue(task3);
		testOk = false;
		try {
			assertTrue("Executing first chained task.", task.running.await(1, TimeUnit.SECONDS));
			assertEquals(2, taskQ.getSizeAfter());
			assertEquals("Both task 2 and 3 need to be removed as they are both chained after task 1", 
					2, taskQ.removeAfter(task).size());
			assertEquals(0, taskQ.getSizeAfter());
			task.finish.countDown();
			assertTrue("Removed first chained task.", task.done.await(1, TimeUnit.SECONDS));
			testOk = true;
		} finally {
			if (!testOk) {
				texec.close();
			}
		}

		texec.close();
	}
}
