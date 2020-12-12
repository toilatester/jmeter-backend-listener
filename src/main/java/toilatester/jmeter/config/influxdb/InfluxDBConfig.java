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


	public static final String DEFAULT_DATABASE = "jmeter";

	public static final String DEFAULT_RETENTION_POLICY = "autogen";

	public static final String DEFAULT_PROTOCOL = "http";

	public static final String DEFAULT_HOST = "localhost";
	
	public static final int DEFAULT_PORT = 8086;

	public static final String KEY_INFLUX_DB_DATABASE = "influxDBDatabase";

	public static final String KEY_INFLUX_DB_PASSWORD = "influxDBPassword";

	public static final String KEY_INFLUX_DB_USER = "influxDBUser";

	public static final String KEY_INFLUX_DB_PROTOCOL = "influxDBProtocol";

	public static final String KEY_INFLUX_DB_HOST = "influxDBHost";

	public static final String KEY_INFLUX_DB_PORT = "influxDBPort";

	public static final String KEY_RETENTION_POLICY = "retentionPolicy";

	private String influxDBProtocol;

	public String getInfluxDBProtocol() {
		return influxDBProtocol;
	}

	public void setInfluxDBProtocol(String influxDBProtocol) {
		this.influxDBProtocol = influxDBProtocol;
	}

	private String influxDBHost;

	private String influxUser;

	private String influxPassword;

	private String influxDatabase;

	private String influxRetentionPolicy;

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

	public String getInfluxDBURL() {
		return influxDBProtocol + "://" + influxDBHost + ":" + influxDBPort;
	}

	public String getInfluxDBHost() {
		return influxDBHost;
	}

	public void setInfluxDBHost(String influxDBHost) {
		this.influxDBHost = influxDBHost;
	}

	
	public String getInfluxUser() {
		return influxUser;
	}


	public void setInfluxUser(String influxUser) {
		this.influxUser = influxUser;
	}

	public String getInfluxPassword() {
		return influxPassword;
	}


	public void setInfluxPassword(String influxPassword) {
		this.influxPassword = influxPassword;
	}


	public String getInfluxDatabase() {
		return influxDatabase;
	}


	public void setInfluxDatabase(String influxDatabase) {
		this.influxDatabase = influxDatabase;
	}


	public String getInfluxRetentionPolicy() {
		return influxRetentionPolicy;
	}


	public void setInfluxRetentionPolicy(String influxRetentionPolicy) {
		this.influxRetentionPolicy = influxRetentionPolicy;
	}


	public int getInfluxDBPort() {
		return influxDBPort;
	}


	public void setInfluxDBPort(int influxDBPort) {
		this.influxDBPort = influxDBPort;
	}
}
