package toilatester.jmeter.config.loki;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

public class LokiDBConfig {
	public static final String DEFAULT_PROTOCOL = "http";
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 3100;
	public static final String DEFAUlT_LOKI_API_ENDPOINT = "/loki/api/v1/push";
	public static final String DEFAUlT_LOKI_EXTERNAL_LABEL = "jmeter_plugin=loki-log";
	public static final int DEFAULT_BATCH_SIZE = 100;
	public static final int DEFAULT_SEND_BATCH_INTERVAL_TIME = 1000;
	public static final long DEFAULT_BATCH_TIMEOUT_MS = 60 * 1000;
	public static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30 * 1000;
	public static final long DEFAULT_REQUEST_TIMEOUT_MS = 30 * 1000;
	public static final boolean DEFAULT_LOG_RESPONSE_BODY_FAILED_SAMPLER_ONLY = true;

	public static final String KEY_LOKI_DB_PROTOCOL = "lokiPortocol";
	public static final String KEY_LOKI_DB_HOST = "lokiHost";
	public static final String KEY_LOKI_DB_PORT = "lokiPort";
	public static final String KEY_LOKI_DB_API_ENDPOINT = "lokiApiEndPoint";
	public static final String KEY_LOKI_DB_BATCH_SIZE = "lokiBatchSize";
	public static final String KEY_LOKI_DB_SEND_BATCH_INTERVAL_TIME = "lokiSendBatchIntervalTime";
	public static final String KEY_LOKI_BATCH_TIMEOUT_MS = "lokiBatchTimeout";
	public static final String KEY_CONNECTION_TIMEOUT_MS = "lokiConnectionTimeout";
	public static final String KEY_REQUEST_TIMEOUT_MS = "lokiRequestTimeout";
	public static final String KEY_LOKI_EXTERNAL_LABELS = "lokiLabels";
	public static final String KEY_LOKI_LOG_ONLY_SAMPLER_RESPONSE_FAILED = "lokiLogResponseBodyFailedSamplerOnly";
	private static final String DEFAULT_DELIMITER_CHAR = ",";
	private String lokiProtocol;
	private String lokiHost;
	private int lokiPort;
	private String lokiApi;
	private int lokibBatchSize;
	private long lokiSendBatchIntervalTime;
	private long lokiBatchTimeout;
	private long lokiConnectiontimeout;
	private long lokiRequestTimeout;
	private boolean lokiLogResponseBodyFailedSamplerOnly;
	private Map<String, String> lokiExternalLabels = new HashMap<String, String>();

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
			throw new IllegalArgumentException(KEY_LOKI_DB_PROTOCOL + " must not be empty!");
		}
		setLokiProtocol(lokiDBProtocolCTX);
	}

	private void initLokiDBHostCTX(BackendListenerContext context) {
		String lokiDBHostCTX = context.getParameter(KEY_LOKI_DB_HOST);
		if (StringUtils.isEmpty(lokiDBHostCTX)) {
			throw new IllegalArgumentException(KEY_LOKI_DB_HOST + " must not be empty!");
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
			throw new IllegalArgumentException(KEY_LOKI_DB_API_ENDPOINT + " must not be empty!");
		}
		setLokiApi(lokiDBApiUrlCTX);
	}

	private void initLokiDBConifgCTX(BackendListenerContext context) {
		long lokiConnectionTimeout = context.getLongParameter(KEY_CONNECTION_TIMEOUT_MS);
		long lokiRequestTimeout = context.getLongParameter(KEY_REQUEST_TIMEOUT_MS);
		int lokiBatchSize = context.getIntParameter(KEY_LOKI_DB_BATCH_SIZE);
		long lokiSendBatchIntervalTime = context.getIntParameter(KEY_LOKI_DB_SEND_BATCH_INTERVAL_TIME);
		long lokiBatchTimeout = context.getLongParameter(KEY_LOKI_BATCH_TIMEOUT_MS);
		boolean lokiLogResponseBodyFailedSamplerOnly = context.getBooleanParameter(
				KEY_LOKI_LOG_ONLY_SAMPLER_RESPONSE_FAILED, DEFAULT_LOG_RESPONSE_BODY_FAILED_SAMPLER_ONLY);
		setLokiConnectiontimeout(lokiConnectionTimeout);
		setLokiRequestTimeout(lokiRequestTimeout);
		setLokibBatchSize(lokiBatchSize);
		setLokiSendBatchIntervalTime(lokiSendBatchIntervalTime);
		setLokiBatchTimeout(lokiBatchTimeout);
		setLokiExternalLabels(this.parsingExternalLabels(context.getParameter(KEY_LOKI_EXTERNAL_LABELS)));
		setLokiLogResponseBodyFailedSamplerOnly(lokiLogResponseBodyFailedSamplerOnly);
	}

	private Map<String, String> parsingExternalLabels(String rawExternalLabel) {
		Map<String, String> externalLokiLabels = new HashMap<String, String>();
		if (StringUtils.isEmpty(rawExternalLabel))
			return externalLokiLabels;
		String[] listlabels = rawExternalLabel.split(DEFAULT_DELIMITER_CHAR);
		for (String label : listlabels) {
			String[] labelValue = label.split("=");
			// if (labelValue.length > 0)
			externalLokiLabels.put(labelValue[0], labelValue[1]);
		}
		return externalLokiLabels;
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

	public Map<String, String> getLokiExternalLabels() {
		return this.lokiExternalLabels;
	}

	public void setLokiExternalLabels(Map<String, String> lokiExternalLabels) {
		this.lokiExternalLabels = lokiExternalLabels;
	}

	public long getLokiSendBatchIntervalTime() {
		return lokiSendBatchIntervalTime;
	}

	public void setLokiSendBatchIntervalTime(long lokiSendBatchIntervalTime) {
		this.lokiSendBatchIntervalTime = lokiSendBatchIntervalTime;
	}

	public boolean isLokiLogResponseBodyFailedSamplerOnly() {
		return lokiLogResponseBodyFailedSamplerOnly;
	}

	public void setLokiLogResponseBodyFailedSamplerOnly(boolean lokiLogResponseBodyFailedSamplerOnly) {
		this.lokiLogResponseBodyFailedSamplerOnly = lokiLogResponseBodyFailedSamplerOnly;
	}

}
