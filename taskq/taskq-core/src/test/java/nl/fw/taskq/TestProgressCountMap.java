package nl.fw.taskq;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestProgressCountMap {

	@Test
	public void testCountMap() {
		
		SyncCountMap<String> cmap = new SyncCountMap<String>();
		cmap.increment(null);
		cmap.increment("one");
		cmap.increment("two");
		cmap.increment("two");
		cmap.increment("three");
		cmap.increment("three");
		cmap.increment("three");
		cmap.increment("three");
		cmap.decrement("three");
		assertEquals(1, cmap.getCount(null));
		assertEquals(1, cmap.getCount("one"));
		assertEquals(2, cmap.getCount("two"));
		assertEquals(3, cmap.getCount("three"));
		cmap.decrement(null);
		cmap.decrement("two");
		cmap.decrement("two");
		assertEquals(0, cmap.getCount(null));
		assertEquals(0, cmap.getCount("two"));
		assertEquals(2, cmap.getSizeKeys());
	}
}
