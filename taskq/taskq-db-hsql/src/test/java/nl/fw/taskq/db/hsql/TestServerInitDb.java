package nl.fw.taskq.db.hsql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import nl.fw.taskq.db.TaskQDbServer;
import nl.fw.taskq.db.TaskQDbInit;
import nl.fw.taskq.db.TQueryNames;
import nl.fw.util.jdbc.INamedQuery;
import nl.fw.util.jdbc.hikari.DbConnHik;
import nl.fw.util.jdbc.hikari.HikPool;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServerInitDb {

	private static final Logger log = LoggerFactory.getLogger(TestServerInitDb.class);
	private static HikPool dbPool = null;

	@BeforeClass
	public static void openDb() throws Exception {

		dbPool = new HikPool();
		//dbPool.setLogPoolUsage(true);
		//dbPool.setReportIntervalMs(10L);
		dbPool.open(dbPool.loadDbProps("db-test.properties", "db.test."));
		log.debug("Database opened.");
	}

	@AfterClass
	public static void closeDb() {

		if (dbPool != null) {
			dbPool.close();
		}
		log.debug("Database closed.");
	}
	
	private INamedQuery taskQueries;
	
	@Test
	public void setupDb() {
		
		DbConnHik dbc = new DbConnHik(dbPool);
		try {
			TaskQDbInit initDb = new TaskQDbInit();
			initDb.initDb(dbc.getConnection());
			taskQueries = initDb.getNamedQueries();
			dbc.setNamedQueries(taskQueries);
			testPropQueries(dbc);
			testServerSetup(dbc);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError(e);
		} finally {
			dbc.rollbackAndClose();
		}
	}

	private void testPropQueries(DbConnHik dbc) throws Exception {
		
		dbc.nameStatement(TQueryNames.GET_PROP).getNamedStatement().setString("key", "taskq.version");
		assertTrue("Have taskq version property", dbc.executeQuery().getResultSet().next());
		String version = dbc.getResultSet().getString(1);
		log.debug("TaskQ version: " + version);
		dbc.nameStatement(TQueryNames.MERGE_PROP).getNamedStatement().setString("key", "test-key");
		dbc.getNamedStatement().setString("value", "test-value");
		assertEquals(1, dbc.executeUpdate().getResultCount());
		dbc.commit();
		dbc.nameStatement(TQueryNames.GET_PROP).getNamedStatement().setString("key", "test-key");
		assertTrue(dbc.executeQuery().getResultSet().next());
		assertEquals("test-value", dbc.getResultSet().getString(1));
		dbc.nameStatement(TQueryNames.MERGE_PROP).getNamedStatement().setString("key", "test-key");
		dbc.getNamedStatement().setString("value", "test-value2");
		assertEquals(1, dbc.executeUpdate().getResultCount());
		dbc.commit();
		dbc.nameStatement(TQueryNames.GET_PROP).getNamedStatement().setString("key", "test-key");
		assertTrue(dbc.executeQuery().getResultSet().next());
		assertEquals("test-value2", dbc.getResultSet().getString(1));
		dbc.nameStatement(TQueryNames.DELETE_PROP).getNamedStatement().setString("key", "test-key");
		assertEquals(1, dbc.executeUpdate().getResultCount());
		dbc.commit();
	}
	
	private void testServerSetup(DbConnHik dbc) throws Exception {
		
		TaskQDbServer tqdb = new TaskQDbServer();
		tqdb.registerServer(dbc);
		dbc.commit();
		assertEquals(1, tqdb.getServerId());
		// updates last active
		tqdb.registerServer(dbc);
		dbc.commit();
	}

}
