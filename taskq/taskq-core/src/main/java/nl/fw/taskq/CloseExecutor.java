package nl.fw.taskq;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

public class CloseExecutor implements ICloseExecutor {

	private final long taskFinishPeriodMs;
	private final long taskStopPeriodMs;
	private final Logger log;
	private boolean interrupted;

	public CloseExecutor(Logger log, long taskFinishPeriodMs, long taskStopPeriodMs) {
		this.log = log;
		this.taskFinishPeriodMs = taskFinishPeriodMs;
		this.taskStopPeriodMs = taskStopPeriodMs;
	}

	@Override
	public void close(ExecutorService executor) {

		executor.shutdown();
		boolean finished = (taskFinishPeriodMs > 0L ? 
				waitForTasks(executor, taskFinishPeriodMs) : 
					executor.isTerminated());
		if (!finished) {
			log.debug("Interrupting running tasks.");
			executor.shutdownNow();
			if (taskStopPeriodMs > 0L && !interrupted) {
				finished = waitForTasks(executor, taskStopPeriodMs);
			}
		}
	}

	protected boolean waitForTasks(ExecutorService executor, long waitTimeMs) {

		long sleepTimeMs = waitTimeMs / 10;
		if (sleepTimeMs < 1000L) {
			sleepTimeMs = 1000L;
			if (sleepTimeMs > waitTimeMs) {
				sleepTimeMs = waitTimeMs;
			}
		}
		long startTime = System.currentTimeMillis();
		while (startTime + waitTimeMs > System.currentTimeMillis()) {
			try {
				executor.awaitTermination(sleepTimeMs, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				log.warn("Waiting for running tasks interrupted: " + e);
				interrupted = true;
				break;
			}
			if (executor.isTerminated()) {
				break;
			} else {
				log.debug("Waiting for running tasks to finish.");
			}
		}
		return executor.isTerminated();
	}

}
