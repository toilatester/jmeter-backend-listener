package toilatester.jmeter.config.loki;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

public class LokiDBConfig {
	public static final String DEFAULT_PROTOCOL = "http";
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 3100;
	public static final String DEFAUlT_LOKI_API_ENDPOINT = "/loki/api/v1/push";
	public static final int DEFAULT_BATCH_SIZE = 1000;
	public static final long DEFAULT_BATCH_TIMEOUT_MS = 5 * 1000;
	public static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30 * 1000;
	public static final long DEFAULT_REQUEST_TIMEOUT_MS = 5 * 1000;

	public static final String KEY_LOKI_DB_PROTOCOL = "lokiPortocol";
	public static final String KEY_LOKI_DB_HOST = "lokiHost";
	public static final String KEY_LOKI_DB_PORT = "lokiPort";
	public static final String KEY_LOKI_DB_API_ENDPOINT = "lokiApiEndPoint";
	public static final String KEY_LOKI_DB_BATCH_SIZE = "lokiBatchSize";
	public static final String KEY_LOKI_BATCH_TIMEOUT_MS = "lokiBatchTimeout";
	public static final String KEY_CONNECTION_TIMEOUT_MS = "lokiConnectionTimeout";
	public static final String KEY_REQUEST_TIMEOUT_MS = "lokiRequestTimeout";

	private String lokiProtocol;
	private String lokiHost;
	private int lokiPort;
	private String lokiApi;
	private int lokibBatchSize;
	private long lokiBatchTimeout;

	private long lokiConnectiontimeout;
	private long lokiRequestTimeout;

	public String getLokiProtocol() {
		return lokiProtocol;
	}

	public void setLokiProtocol(String lokiProtocol) {
		this.lokiProtocol = lokiProtocol;
	}

	public String getLokiHost() {
		return lokiHost;
	}

	public void setLokiHost(String lokiHost) {
		this.lokiHost = lokiHost;
	}

	public int getLokiPort() {
		return lokiPort;
	}

	public void setLokiPort(int lokiPort) {
		this.lokiPort = lokiPort;
	}

	public int getLokibBatchSize() {
		return lokibBatchSize;
	}

	public void setLokibBatchSize(int lokibBatchSize) {
		this.lokibBatchSize = lokibBatchSize;
	}

	public long getLokiBatchTimeout() {
		return lokiBatchTimeout;
	}

	public void setLokiBatchTimeout(long lokiBatchTimeout) {
		this.lokiBatchTimeout = lokiBatchTimeout;
	}

	public long getLokiConnectiontimeout() {
		return lokiConnectiontimeout;
	}

	public void setLokiConnectiontimeout(long lokiConnectiontimeout) {
		this.lokiConnectiontimeout = lokiConnectiontimeout;
	}

	public long getLokiRequestTimeout() {
		return lokiRequestTimeout;
	}

	public void setLokiRequestTimeout(long lokiRequestTimeout) {
		this.lokiRequestTimeout = lokiRequestTimeout;
	}

	public LokiDBConfig(BackendListenerContext context) {
		initLokiDBProtocolCTX(context);
		initLokiDBHostCTX(context);
		initLokiDBPortCTX(context);
		initLokiDBApiUrlCTX(context);
		initLokiDBConifgCTX(context);
	}

	private void initLokiDBProtocolCTX(BackendListenerContext context) {
		String lokiDBProtocolCTX = context.getParameter(KEY_LOKI_DB_PROTOCOL);
		if (StringUtils.isEmpty(lokiDBProtocolCTX)) {
			throw new IllegalArgumentException(KEY_LOKI_DB_PROTOCOL + "must not be empty!");
		}
		setLokiProtocol(lokiDBProtocolCTX);
	}

	private void initLokiDBHostCTX(BackendListenerContext context) {
		String lokiDBHostCTX = context.getParameter(KEY_LOKI_DB_HOST);
		if (StringUtils.isEmpty(lokiDBHostCTX)) {
			throw new IllegalArgumentException(KEY_LOKI_DB_HOST + "must not be empty!");
		}
		setLokiHost(lokiDBHostCTX);
	}

	private void initLokiDBPortCTX(BackendListenerContext context) {
		int lokiDBPortCTX = context.getIntParameter(KEY_LOKI_DB_PORT);
		setLokiPort(lokiDBPortCTX);
	}

	private void initLokiDBApiUrlCTX(BackendListenerContext context) {
		String lokiDBApiUrlCTX = context.getParameter(KEY_LOKI_DB_API_ENDPOINT);
		if (StringUtils.isEmpty(lokiDBApiUrlCTX)) {
			throw new IllegalArgumentException(KEY_LOKI_DB_API_ENDPOINT + "must not be empty!");
		}
		setLokiApi(lokiDBApiUrlCTX);
	}

	private void initLokiDBConifgCTX(BackendListenerContext context) {
		long lokiConnectionTimeout = context.getLongParameter(KEY_CONNECTION_TIMEOUT_MS);
		long lokiRequestTimeout = context.getLongParameter(KEY_REQUEST_TIMEOUT_MS);
		int lokiBatchSize = context.getIntParameter(KEY_LOKI_DB_BATCH_SIZE);
		long lokiBatchTimeout = context.getLongParameter(KEY_LOKI_BATCH_TIMEOUT_MS);
		setLokiConnectiontimeout(lokiConnectionTimeout);
		setLokiRequestTimeout(lokiRequestTimeout);
		setLokibBatchSize(lokiBatchSize);
		setLokiBatchTimeout(lokiBatchTimeout);
	}

	public String getLokiUrl() {
		return getLokiProtocol() + "://" + getLokiHost() + ":" + getLokiPort() + getLokiApi();
	}

	public String getLokiApi() {
		return lokiApi;
	}

	public void setLokiApi(String lokiApi) {
		this.lokiApi = lokiApi;
	}

}
