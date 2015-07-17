package nl.fw.taskq.file;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.fw.taskq.TaskQGroup;
import nl.fw.taskq.util.FileUtil;

public class FileTaskQ<TASK_TYPE> extends TaskQGroup<TASK_TYPE, Long> {
	
	private static final Logger log = LoggerFactory.getLogger(FileTaskQ.class);

	private Path baseDir = new File(FileUtil.TmpDir).toPath();
	private final AtomicLong taskIdCount = new AtomicLong();
	
	public void setLastTaskId(long lastId) {
		taskIdCount.set(lastId);
	}
	
	public void setBaseDir(Path baseDir) {
		this.baseDir = baseDir;
	}
	
	public <QTT extends TASK_TYPE> Path storeTask(String qname, QTT task, Long taskId) throws IOException {
		
		Path qdir = baseDir.resolve(qname);
		Files.createDirectories(qdir);
		Path taskFile = qdir.resolve(taskId + ".dat");
		try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(taskFile))) {
			out.writeObject((Serializable)task);
		}
		if (log.isDebugEnabled()) {
			log.debug("Stored task [" + taskId + "] for queue [" + qname + "]");
		}
		return taskFile;
	}
	
	@SuppressWarnings("unchecked")
	public TASK_TYPE loadTask(String qname, Long taskId) throws IOException {
		
		Path taskFile = baseDir.resolve(qname).resolve(taskId + ".dat");
		TASK_TYPE task = null;
		try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(taskFile))) {
			try {
				task = (TASK_TYPE) in.readObject();
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Unable to load task [" + taskId + "] for queue [" + qname +"]", e);
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("Loaded task [" + taskId + "] for queue [" + qname + "]");
		}
		return task;
	}
	
	public void deleteTask(String qname, Long taskId) throws IOException {
		
		Path taskFile = baseDir.resolve(qname).resolve(taskId + ".dat");
		Files.deleteIfExists(taskFile);
	}
	
	public Long loadAllTasks() throws IOException {
		
		Long lastId = 0L;
		for (String qname : getTaskQNames()) {
			
			Path qdir = baseDir.resolve(qname);
			if (!Files.exists(qdir)) {
				continue;
			}
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(baseDir.resolve(qname))) {
				Iterator<Path> dirIt = dirStream.iterator();
				while (dirIt.hasNext()) {
					Path p = dirIt.next();
					String fname = p.getFileName().toString();
					if (fname.indexOf('.') > 0 && fname.endsWith(".dat")) {
						Long taskId = null;
						try {
							taskId = Long.valueOf(fname.substring(0, fname.indexOf('.')));
						} catch (Exception ignored) {}
						if (taskId != null) {
							getTaskQ(qname).enqueue(loadTask(qname, taskId));
							if (taskId > lastId) {
								lastId = taskId;
							}
						}
					}
				}
			}
		}
		return lastId;
	}
	
	@Override
	public <QTT extends TASK_TYPE> Long getTaskId(QTT task) {
		return taskIdCount.incrementAndGet();
	}

}
