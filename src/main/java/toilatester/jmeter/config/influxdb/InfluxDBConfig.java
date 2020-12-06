package toilatester.jmeter.config.influxdb;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

/**
 * Configuration for influxDB.
 * 
 * @author minhhoang
 *
 */
public class InfluxDBConfig {

	/**
	 * Default database name.
	 */
	public static final String DEFAULT_DATABASE = "jmeter";

	/**
	 * Default retention policy name.
	 */
	public static final String DEFAULT_RETENTION_POLICY = "autogen";

	/**
	 * Default protocol.
	 */
	public static final String DEFAULT_PROTOCOL = "http";
	/**
	 * Default host name.
	 */
	public static final String DEFAULT_HOST = "localhost";
	
	/**
	 * Default port.
	 */
	public static final int DEFAULT_PORT = 8086;

	/**
	 * Config key for database name.
	 */
	public static final String KEY_INFLUX_DB_DATABASE = "influxDBDatabase";

	/**
	 * Config key for password.
	 */
	public static final String KEY_INFLUX_DB_PASSWORD = "influxDBPassword";

	/**
	 * Config key for user name.
	 */
	public static final String KEY_INFLUX_DB_USER = "influxDBUser";
	/**
	 * Config key for protocol.
	 */
	public static final String KEY_INFLUX_DB_PROTOCOL = "influxDBProtocol";
	/**
	 * Config key for host.
	 */
	public static final String KEY_INFLUX_DB_HOST = "influxDBHost";

	/**
	 * Config key for port.
	 */
	public static final String KEY_INFLUX_DB_PORT = "influxDBPort";

	/**
	 * Config key for retention policy name.
	 */
	public static final String KEY_RETENTION_POLICY = "retentionPolicy";

	/**
	 * InfluxDB Protocol.
	 */
	private String influxDBProtocol;

	public String getInfluxDBProtocol() {
		return influxDBProtocol;
	}

	public void setInfluxDBProtocol(String influxDBProtocol) {
		this.influxDBProtocol = influxDBProtocol;
	}

	/**
	 * InfluxDB Host.
	 */
	private String influxDBHost;

	/**
	 * InfluxDB User.
	 */
	private String influxUser;

	/**
	 * InfluxDB Password.
	 */
	private String influxPassword;

	/**
	 * InfluxDB database name.
	 */
	private String influxDatabase;

	/**
	 * InfluxDB database retention policy.
	 */
	private String influxRetentionPolicy;

	/**
	 * InfluxDB Port.
	 */
	private int influxDBPort;

	public InfluxDBConfig(BackendListenerContext context) {
		initInfluxDBProtocolCTX(context);
		initInfluxDBHostCTX(context);
		initInfluxDbConnection(context);
		initInfluxDbOptions(context);
	}

	private void initInfluxDBProtocolCTX(BackendListenerContext context) {
		String influxDBProtocolCTX = context.getParameter(KEY_INFLUX_DB_PROTOCOL);
		if (StringUtils.isEmpty(influxDBProtocolCTX)) {
			throw new IllegalArgumentException(KEY_INFLUX_DB_PROTOCOL + "must not be empty!");
		}
		setInfluxDBProtocol(influxDBProtocolCTX);
	}

	private void initInfluxDBHostCTX(BackendListenerContext context) {
		String influxDBHostCTX = context.getParameter(KEY_INFLUX_DB_HOST);
		if (StringUtils.isEmpty(influxDBHostCTX)) {
			throw new IllegalArgumentException(KEY_INFLUX_DB_HOST + "must not be empty!");
		}
		setInfluxDBHost(influxDBHostCTX);
	}

	private void initInfluxDbConnection(BackendListenerContext context) {
		String influxDBProtocolCTX = context.getParameter(KEY_INFLUX_DB_PROTOCOL, InfluxDBConfig.DEFAULT_PROTOCOL);
		setInfluxDBProtocol(influxDBProtocolCTX);
		
		int influxDBPortCTX = context.getIntParameter(KEY_INFLUX_DB_PORT, InfluxDBConfig.DEFAULT_PORT);
		setInfluxDBPort(influxDBPortCTX);

		String influxUserCTX = context.getParameter(KEY_INFLUX_DB_USER, "default");
		setInfluxUser(influxUserCTX);

		String influxPasswordCTX = context.getParameter(KEY_INFLUX_DB_PASSWORD, "default");
		setInfluxPassword(influxPasswordCTX);
	}

	private void initInfluxDbOptions(BackendListenerContext context) {
		String influxDatabaseCTX = context.getParameter(KEY_INFLUX_DB_DATABASE);
		if (StringUtils.isEmpty(influxDatabaseCTX)) {
			throw new IllegalArgumentException(KEY_INFLUX_DB_DATABASE + "must not be empty!");
		}
		setInfluxDatabase(influxDatabaseCTX);

		String influxRetentionPolicyCTX = context.getParameter(KEY_RETENTION_POLICY, DEFAULT_RETENTION_POLICY);
		if (StringUtils.isEmpty(influxRetentionPolicyCTX)) {
			influxRetentionPolicyCTX = DEFAULT_RETENTION_POLICY;
		}
		setInfluxRetentionPolicy(influxRetentionPolicyCTX);
	}

	/**
	 * Builds URL to influxDB.
	 * 
	 * @return influxDB URL.
	 */
	public String getInfluxDBURL() {
		return influxDBProtocol + "://" + influxDBHost + ":" + influxDBPort;
	}

	/**
	 * @return the influxDBHost
	 */
	public String getInfluxDBHost() {
		return influxDBHost;
	}

	/**
	 * @param influxDBHost the influxDBHost to set
	 */
	public void setInfluxDBHost(String influxDBHost) {
		this.influxDBHost = influxDBHost;
	}

	/**
	 * @return the influxUser
	 */
	public String getInfluxUser() {
		return influxUser;
	}

	/**
	 * @param influxUser the influxUser to set
	 */
	public void setInfluxUser(String influxUser) {
		this.influxUser = influxUser;
	}

	/**
	 * @return the influxPassword
	 */
	public String getInfluxPassword() {
		return influxPassword;
	}

	/**
	 * @param influxPassword the influxPassword to set
	 */
	public void setInfluxPassword(String influxPassword) {
		this.influxPassword = influxPassword;
	}

	/**
	 * @return the influxDatabase
	 */
	public String getInfluxDatabase() {
		return influxDatabase;
	}

	/**
	 * @param influxDatabase the influxDatabase to set
	 */
	public void setInfluxDatabase(String influxDatabase) {
		this.influxDatabase = influxDatabase;
	}

	/**
	 * @return the influxRetentionPolicy
	 */
	public String getInfluxRetentionPolicy() {
		return influxRetentionPolicy;
	}

	/**
	 * @param influxRetentionPolicy the influxRetentionPolicy to set
	 */
	public void setInfluxRetentionPolicy(String influxRetentionPolicy) {
		this.influxRetentionPolicy = influxRetentionPolicy;
	}

	/**
	 * @return the influxDBPort
	 */
	public int getInfluxDBPort() {
		return influxDBPort;
	}

	/**
	 * @param influxDBPort the influxDBPort to set
	 */
	public void setInfluxDBPort(int influxDBPort) {
		this.influxDBPort = influxDBPort;
	}
}
