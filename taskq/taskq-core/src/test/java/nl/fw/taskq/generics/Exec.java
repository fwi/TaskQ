package nl.fw.taskq.generics;

public interface Exec<T> {
	
	<U extends T> void add(Queue<U> q);

}
