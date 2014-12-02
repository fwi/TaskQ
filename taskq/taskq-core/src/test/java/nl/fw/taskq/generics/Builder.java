package nl.fw.taskq.generics;

public class Builder {

	public static void build() {
		
		Exec<Runnable> e = new Exec<Runnable>() {

			@Override
			public <U extends Runnable> void add(Queue<U> q) {}
		};
		
		Queue<Item> q = new Queue<Item>() {
			@Override public void add(Item i) {}
		};
		q.add(new Item());
		q.add(new Item2());
		e.add(q);
		
	}
}
