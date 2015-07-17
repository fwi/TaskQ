package nl.fw.taskq.db;

import java.sql.Connection;

import nl.fw.util.jdbc.DbConn;
import nl.fw.util.jdbc.DbConnNamedStatement;
import nl.fw.util.jdbc.DbConnUtil;
import nl.fw.util.jdbc.INamedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskQDbServer {

	private static final Logger log = LoggerFactory.getLogger(TaskQDbServer.class);

	public static final String DEFAULT_SERVER_HOST = "localhost";
	public static final int DEFAULT_SERVER_PORT = 9321;
	public static final String DEFAULT_SERVER_GROUP = "default";

	public String serverHost = DEFAULT_SERVER_HOST;
	public int serverPort = DEFAULT_SERVER_PORT;
	public String serverGroup = DEFAULT_SERVER_GROUP;
	
	private int serverId = -1;
	protected TaskQDbServerDao dao = new TaskQDbServerDao();
	
	public TaskQDbServer() {
		determineLocalHostname();
	}
	
	protected void determineLocalHostname() {
		
		try {
			java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
			String ip = localHost.getHostAddress();
			String name = localHost.getCanonicalHostName();
			if (ip.equals(name)) {
				name = localHost.getHostName();
			}
			serverHost = name;
			log.info("Found server host {}", serverHost);
		} catch (Exception e) {
			log.warn("Could not use local hostname as server name - " + e);
		}
	}
	
	public String getServerName() {
		return serverHost + ":" + serverPort;
	}
	
	public int getServerId() {
		return serverId;
	}
	
	public void registerServer(Connection c, INamedQuery namedQueries) {
		registerServer(new DbConn(c).setNamedQueries(namedQueries));
	}
	
	public void registerServer(DbConnNamedStatement<?> c) {
		
		try {
			boolean exists = false;
			serverId = dao.setConn(c).findServer(getServerName(), serverGroup);
			if (serverId > -1) {
				exists = true;
				dao.updateActive(serverId);
			} else {
				serverId = dao.insertServer(getServerName(), serverGroup);
			}
			c.commit();
			log.info("{} server ID {} for {} in group {}", (exists ? "Updated" : "Registered"), serverId, getServerName(), serverGroup);
		} catch (Exception e) {
			c.rollbackSilent();
			DbConnUtil.rethrowRuntime(e);
		}
	}
	
	public void updateActive(Connection c, INamedQuery namedQueries) {
		updateActive(new DbConn(c).setNamedQueries(namedQueries));
	}
	
	public void updateActive(DbConnNamedStatement<?> c) {
		
		try {
			dao.setConn(c).updateActive(getServerId());
			c.commit();
		} catch (Exception e) {
			c.rollbackSilent();
			DbConnUtil.rethrowRuntime(e);
		}
	}
	
}
