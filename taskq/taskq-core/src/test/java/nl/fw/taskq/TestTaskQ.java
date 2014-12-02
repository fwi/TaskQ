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
public class TestTaskQ {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Test
	public void testTaskQ() throws Exception {
		
		// Check execution of one task
		TaskQCountDown taskQ = new TaskQCountDown();
		CountDownTask task = new CountDownTask();
		taskQ.enqueue(task);
		TaskExec<Runnable> texec = new TaskExec<Runnable>();
		texec.add(taskQ);
		texec.start();
		boolean testOk = false;
		try {
			assertTrue("Executing first task.", task.running.await(1, TimeUnit.SECONDS));
			task.finish.countDown();
			assertTrue("Removing first task.", task.done.await(1, TimeUnit.SECONDS));
			testOk = true;
		} finally {
			if (!testOk) {
				texec.close();
			}
		}
		
		// Check max amount tasks concurrent, add MaxInProgress + 1 tasks to queue.
		CountDownLatch taskRun = new CountDownLatch(taskQ.getMaxInProgress());
		CountDownLatch taskDone = new CountDownLatch(taskQ.getMaxInProgress());
		CountDownLatch taskFinish = new CountDownLatch(1);
		for (int i = 0; i < taskQ.getMaxInProgress(); i++) {
			task = new CountDownTask();
			task.running = taskRun;
			task.finish = taskFinish;
			task.done = taskDone;
			taskQ.enqueue(task);
		}
		// When max amount tasks finished, next task should be picked up.
		task = new CountDownTask();
		taskQ.enqueue(task);
		testOk = false;
		try {
			assertTrue("Executing tasks.", taskRun.await(1, TimeUnit.SECONDS));
			assertEquals("No more than max tasks running at the same time.", taskQ.getMaxInProgress(), taskQ.getInProgressCount());
			taskFinish.countDown();
			assertTrue("Removing tasks.", taskDone.await(1, TimeUnit.SECONDS));
			assertTrue("Executing second task.", task.running.await(1, TimeUnit.SECONDS));
			assertEquals("Previous tasks are finished", 1, taskQ.getInProgressCount());
			task.finish.countDown();
			testOk = true;
		} finally {
			if (!testOk) {
				texec.close();
			}
		}
		texec.close();
	}
	
}
