package nl.fw.taskq.file;

import java.io.IOException;
import java.nio.file.Path;

import nl.fw.taskq.file.FileTaskQ;

public class FileTestTaskQGroup extends FileTaskQ<FileTask> {

	@Override 
	public FileTask loadTask(String qname, Long taskId) throws IOException {
		
		FileTask task = super.loadTask(qname, taskId);
		task.qgroup = this;
		return task;
	}
	
	@Override 
	public Path storeTask(String qname, FileTask task, Long taskId) throws IOException {
		
		task.id = taskId;
		task.qname = qname;
		return super.storeTask(qname, task, taskId);
	}

}
