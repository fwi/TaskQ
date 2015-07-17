package nl.fw.taskq.file;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
// import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class FileTask implements Runnable, Serializable {

	private static final Logger log = LoggerFactory.getLogger(FileTask.class);

	public transient FileTestTaskQGroup qgroup;
	public Long id;
	public String qname;
	// public Map<String, Object> taskData;

	public transient CountDownLatch storeDone;
	public transient CountDownLatch deleteDone;

	public FileTask() {
		initTransient();
	}
	
	private void initTransient() {
		storeDone = new CountDownLatch(1);
		deleteDone = new CountDownLatch(1);
	}

	/**
	 * Called by ObjectInputStream, used to initialize transient variables.
	 * These are NOT initialized through the (default) constructor.
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		
		in.defaultReadObject();
		initTransient();
	}
	
	@Override
	public void run() {

		try {
			if ("qtwo".equals(qname)) {
				qgroup.deleteTask(qname, id);
				log.debug("Deleted task " + id + " for queue " + qname);
				deleteDone.countDown();
			} else {
				Path p = qgroup.storeTask("qtwo", this, qgroup.getTaskId(this));
				log.debug("Stored task " + id + " in " + p);
				storeDone.countDown();
			}
		} catch (Exception e) {
			throw new AssertionError("Failed to store task.", e);
		}
	}

}
