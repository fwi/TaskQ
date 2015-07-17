package nl.fw.taskq.db;

import java.sql.SQLException;

import nl.fw.util.jdbc.DbConnNamedStatement;

public class TaskQDbServerDao {
	
	private DbConnNamedStatement<?> c;
	
	public TaskQDbServerDao setConn(DbConnNamedStatement<?> c) {
		this.c = c;
		return this;
	}

	public int findServer(String name, String group) throws SQLException {
		
		c.nameStatement(TQueryNames.FIND_SERVER);
		c.getNamedStatement().setString("name", name);
		c.getNamedStatement().setString("group", group);
		c.executeQuery();
		return (c.getResultSet().next() ? c.getResultSet().getInt("id") : -1);
	}
	
	public int insertServer(String name, String group) throws SQLException {
		
		c.nameStatement(TQueryNames.INSERT_SERVER, true);
		c.getNamedStatement().setString("name", name);
		c.getNamedStatement().setString("group", group);
		c.getNamedStatement().setTimestamp("lastActive", getLastActive());
		c.executeUpdate();
		return (c.getResultSet().next() ? c.getResultSet().getInt(1) : -1);
	}
	
	public int updateActive(int serverId) throws SQLException {
		
		c.nameStatement(TQueryNames.SERVER_ACTIVE);
		c.getNamedStatement().setInt("id", serverId);
		c.getNamedStatement().setTimestamp("lastActive", getLastActive());
		c.getNamedStatement().getStatement().setQueryTimeout(15);
		return c.executeUpdate().getResultCount();
	}
	
	protected java.sql.Timestamp getLastActive() {
		return new java.sql.Timestamp(System.currentTimeMillis());
	}

}
