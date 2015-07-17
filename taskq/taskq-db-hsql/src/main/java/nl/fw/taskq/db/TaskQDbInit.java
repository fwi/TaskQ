package nl.fw.taskq.db;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.fw.util.jdbc.DbConn;
import nl.fw.util.jdbc.DbConnNamedStatement;
import nl.fw.util.jdbc.DbConnUtil;
import nl.fw.util.jdbc.INamedQuery;
import nl.fw.util.jdbc.NamedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to initialize the TaskQ database.
 * <br>Resource strings starting with "file:" are treated as a URI.
 * <br>Methods in this class do NOT close a given database connection.
 * @author fred
 *
 */
public class TaskQDbInit {

	private static final Logger log = LoggerFactory.getLogger(TaskQDbInit.class);

	public String namedQueriesResource = "taskq-db-queries.sql";
	public String dbStructResource = "taskq-db-struct-hsqldb.sql";
	public String dbInitQueriesResource = "taskq-db-data-init.sql";
	public Charset resourceCharset = StandardCharsets.UTF_8;
	
	protected INamedQuery namedQueries;
	protected boolean wasInitialized;
	
	public void initDb(Connection c) {
		initDb(new DbConn(c));
	}

	public void initDb(DbConnNamedStatement<?> c) {
		
		setNamedQueries(loadNamedQueries());
		initStruct(c);
	}
	
	public INamedQuery getNamedQueries() {
		return namedQueries;
	}
	
	public boolean wasInitialized() {
		return wasInitialized;
	}

	public void setNamedQueries(INamedQuery namedQueries) {
		this.namedQueries = namedQueries;
	}

	public NamedQuery loadNamedQueries() {
		
		NamedQuery nq = null;
		InputStream in = null;
		try {
			in = getInputStream(namedQueriesResource);
			nq = new NamedQuery(NamedQuery.loadQueries(getReader(in)));
			if (TQueryNames.GET_PROP.equals(nq.getQuery(TQueryNames.GET_PROP))) {
				throw new IllegalArgumentException("Queries loaded from [" + namedQueriesResource + "] do not contain named TaskQ queries.");
			}
		} catch (Exception e) {
			DbConnUtil.rethrowRuntime(e);
		} finally {
			DbConnUtil.closeSilent(in);
		}
		log.debug("TaskQ named queries loaded from {}", namedQueriesResource);
		return nq;
	}

	protected InputStream getInputStream(String resourceName) throws Exception {
		
		InputStream in = null;
		if (resourceName.startsWith("file:")) {
			in = new URI(resourceName).toURL().openStream();
		} else {
			in = DbConnUtil.getResourceAsStream(resourceName);
		}
		if (in == null) {
			throw new FileNotFoundException("Cannot find resource to open: " + resourceName);
		}
		return in;
	}
	
	protected Reader getReader(InputStream in) {
		return new BufferedReader(
				new InputStreamReader(in, resourceCharset));
	}

	public void initStruct(Connection conn) {
		initStruct(new DbConn(conn));
	}
	
	public void initStruct(DbConnNamedStatement<?> c) {
		
		c.setNamedQueries(getNamedQueries());
		if (isAlreadyInitialized(c)) {
			log.debug("TaskQ database already initialized.");
			return;
		}
		log.debug("Initializing TaskQ database.");
		InputStream in = null;
		try {
			in = getInputStream(dbStructResource);
			loadAndUpdate(c, in);
			in.close();
			in = getInputStream(dbInitQueriesResource);
			loadAndUpdate(c, in);
			wasInitialized = true;
		} catch (Exception e) {
			DbConnUtil.rethrowRuntime(e);
		} finally {
			DbConnUtil.closeSilent(in);
		}
		log.debug("TaskQ database initialized.");
	}
	
	/**
	 * Performs a "get property value" query to see if the database already has TaskQ tables.
	 */
	protected boolean isAlreadyInitialized(DbConnNamedStatement<?> c) {
		
		boolean haveStruct = false;
		try {
			c.nameStatement(TQueryNames.GET_PROP).getNamedStatement().setString("key", "init-test");
			c.executeQuery();
			c.commit();
			haveStruct = true;
		} catch (Exception ignored) {
			// there is no structure
			c.rollbackSilent();
		}
		return haveStruct;
	}
	
	protected void loadAndUpdate(DbConnNamedStatement<?> c, InputStream in) {
		
		try {
			LinkedHashMap<String, String> qmap = NamedQuery.loadQueries(getReader(in));
			for (Map.Entry<String, String> sqlEntry : qmap.entrySet()) {
				log.trace("Executing sql query {}", sqlEntry.getKey());
				c.createStatement().executeUpdate(sqlEntry.getValue());
			}
			c.commit();
		} catch (Exception e) {
			c.rollbackSilent();
			DbConnUtil.rethrowRuntime(e);
		}
	}

}
