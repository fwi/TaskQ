package nl.fw.taskq;

import java.util.concurrent.ExecutorService;

public interface ICloseExecutor {

	void close(ExecutorService executor);
}
