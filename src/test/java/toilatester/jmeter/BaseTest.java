package toilatester.jmeter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import toilatester.jmeter.config.influxdb.InfluxDBConfig;
import toilatester.jmeter.config.loki.LokiClientThreadFactory;
import toilatester.jmeter.config.loki.LokiDBConfig;
import toilatester.jmeter.utils.LokiMockServer;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class BaseTest {

	protected LokiMockServer lokiMockServer;

	protected ExecutorService sendLogThreadPool;
	protected ExecutorService httpClientThreadPool;

	@BeforeEach
	public void beforeEach() {
		this.lokiMockServer = new LokiMockServer();
		this.lokiMockServer.startServer();
		this.sendLogThreadPool = Executors.newFixedThreadPool(1, new LokiClientThreadFactory("jmeter-send-loki-log"));
		this.httpClientThreadPool = new ThreadPoolExecutor(5, Integer.MAX_VALUE, 60000 * 10, TimeUnit.MILLISECONDS,
				new SynchronousQueue<Runnable>(), new LokiClientThreadFactory("jmeter-loki-java-http"));
	}

	@AfterEach
	public void afterEach() {
		this.lokiMockServer.reset();
		this.lokiMockServer.stopServer();
	}

	protected String getLokiHttpMockServerUrl() {
		return "http://localhost:3100/loki/api/v1/push";
	}

	protected String getLokiHttpsMockServerUrl() {
		return "https://localhost:3101/loki/api/v1/push";
	}

	protected LokiDBConfig lokiDbConfig(Map<String, String> params) {

		return new LokiDBConfig(this.backendListenerContext(params));
	}

	protected BackendListenerContext backendListenerContext(Map<String, String> params) {
		return new BackendListenerContext(params);
	}

	protected Map<String, String> defaultInfluxDBConfig() {
		Map<String, String> config = new HashMap<String, String>();
		config.put(InfluxDBConfig.KEY_TEST_NAME, "Test");
		config.put(InfluxDBConfig.KEY_NODE_NAME, "Test-Node");
		config.put(InfluxDBConfig.KEY_INFLUX_DB_PROTOCOL, InfluxDBConfig.DEFAULT_PROTOCOL);
		config.put(InfluxDBConfig.KEY_INFLUX_DB_HOST, InfluxDBConfig.DEFAULT_HOST);
		config.put(InfluxDBConfig.KEY_INFLUX_DB_PORT, Integer.toString(InfluxDBConfig.DEFAULT_PORT));
		config.put(InfluxDBConfig.KEY_INFLUX_DB_USER, "");
		config.put(InfluxDBConfig.KEY_INFLUX_DB_PASSWORD, "");
		config.put(InfluxDBConfig.KEY_INFLUX_DB_DATABASE, InfluxDBConfig.DEFAULT_DATABASE);
		config.put(InfluxDBConfig.KEY_RETENTION_POLICY, InfluxDBConfig.DEFAULT_RETENTION_POLICY);
		config.put(InfluxDBConfig.KEY_SAMPLERS_LIST, ".*");
		config.put(InfluxDBConfig.KEY_USE_REGEX_FOR_SAMPLER_LIST, "true");
		config.put(InfluxDBConfig.KEY_RECORD_SUB_SAMPLES, "true");
		return config;
	}

	protected Map<String, String> lokiConfigWithTurnOffThreadMetrics() {
		Map<String, String> config = new HashMap<String, String>();
		config.put(LokiDBConfig.KEY_LOKI_DB_PROTOCOL, LokiDBConfig.DEFAULT_PROTOCOL);
		config.put(LokiDBConfig.KEY_LOKI_DB_HOST, LokiDBConfig.DEFAULT_HOST);
		config.put(LokiDBConfig.KEY_LOKI_DB_PORT, Integer.toString(LokiDBConfig.DEFAULT_PORT));
		config.put(LokiDBConfig.KEY_LOKI_DB_API_ENDPOINT, LokiDBConfig.DEFAUlT_LOKI_API_ENDPOINT);
		config.put(LokiDBConfig.KEY_LOKI_DB_BATCH_SIZE, Integer.toString(LokiDBConfig.DEFAULT_BATCH_SIZE));
		config.put(LokiDBConfig.KEY_LOKI_EXTERNAL_LABELS, LokiDBConfig.DEFAUlT_LOKI_EXTERNAL_LABEL);
		config.put(LokiDBConfig.KEY_LOKI_DB_SEND_BATCH_INTERVAL_TIME,
				Long.toString(LokiDBConfig.DEFAULT_SEND_BATCH_INTERVAL_TIME));
		config.put(LokiDBConfig.KEY_LOKI_DB_SEND_THREAD_METRICS_INTERVAL_TIME,
				Long.toString(30000));
		config.put(LokiDBConfig.KEY_LOKI_LOG_ONLY_SAMPLER_RESPONSE_FAILED,
				Boolean.toString(LokiDBConfig.DEFAULT_LOG_RESPONSE_BODY_FAILED_SAMPLER_ONLY));
		config.put(LokiDBConfig.KEY_LOKI_BATCH_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_BATCH_TIMEOUT_MS));
		config.put(LokiDBConfig.KEY_CONNECTION_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_CONNECTION_TIMEOUT_MS));
		config.put(LokiDBConfig.KEY_REQUEST_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_REQUEST_TIMEOUT_MS));
		return config;
	}
	
	protected Map<String, String> defaultLokiConfig() {
		Map<String, String> config = new HashMap<String, String>();
		config.put(LokiDBConfig.KEY_LOKI_DB_PROTOCOL, LokiDBConfig.DEFAULT_PROTOCOL);
		config.put(LokiDBConfig.KEY_LOKI_DB_HOST, LokiDBConfig.DEFAULT_HOST);
		config.put(LokiDBConfig.KEY_LOKI_DB_PORT, Integer.toString(LokiDBConfig.DEFAULT_PORT));
		config.put(LokiDBConfig.KEY_LOKI_DB_API_ENDPOINT, LokiDBConfig.DEFAUlT_LOKI_API_ENDPOINT);
		config.put(LokiDBConfig.KEY_LOKI_DB_BATCH_SIZE, Integer.toString(LokiDBConfig.DEFAULT_BATCH_SIZE));
		config.put(LokiDBConfig.KEY_LOKI_EXTERNAL_LABELS, LokiDBConfig.DEFAUlT_LOKI_EXTERNAL_LABEL);
		config.put(LokiDBConfig.KEY_LOKI_DB_SEND_BATCH_INTERVAL_TIME,
				Long.toString(LokiDBConfig.DEFAULT_SEND_BATCH_INTERVAL_TIME));
		config.put(LokiDBConfig.KEY_LOKI_DB_SEND_THREAD_METRICS_INTERVAL_TIME,
				Long.toString(LokiDBConfig.DEFAULT_SEND_THREAD_METRICS_INTERVAL_TIME));
		config.put(LokiDBConfig.KEY_LOKI_LOG_ONLY_SAMPLER_RESPONSE_FAILED,
				Boolean.toString(LokiDBConfig.DEFAULT_LOG_RESPONSE_BODY_FAILED_SAMPLER_ONLY));
		config.put(LokiDBConfig.KEY_LOKI_BATCH_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_BATCH_TIMEOUT_MS));
		config.put(LokiDBConfig.KEY_CONNECTION_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_CONNECTION_TIMEOUT_MS));
		config.put(LokiDBConfig.KEY_REQUEST_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_REQUEST_TIMEOUT_MS));
		return config;
	}

	protected Map<String, String> lokiConfigWithLogAllResponseData() {
		Map<String, String> config = new HashMap<String, String>();
		config.put(LokiDBConfig.KEY_LOKI_DB_PROTOCOL, LokiDBConfig.DEFAULT_PROTOCOL);
		config.put(LokiDBConfig.KEY_LOKI_DB_HOST, LokiDBConfig.DEFAULT_HOST);
		config.put(LokiDBConfig.KEY_LOKI_DB_PORT, Integer.toString(LokiDBConfig.DEFAULT_PORT));
		config.put(LokiDBConfig.KEY_LOKI_DB_API_ENDPOINT, LokiDBConfig.DEFAUlT_LOKI_API_ENDPOINT);
		config.put(LokiDBConfig.KEY_LOKI_DB_BATCH_SIZE, Integer.toString(LokiDBConfig.DEFAULT_BATCH_SIZE));
		config.put(LokiDBConfig.KEY_LOKI_EXTERNAL_LABELS, LokiDBConfig.DEFAUlT_LOKI_EXTERNAL_LABEL);
		config.put(LokiDBConfig.KEY_LOKI_DB_SEND_BATCH_INTERVAL_TIME,
				Long.toString(LokiDBConfig.DEFAULT_SEND_BATCH_INTERVAL_TIME));
		config.put(LokiDBConfig.KEY_LOKI_DB_SEND_THREAD_METRICS_INTERVAL_TIME,
				Long.toString(LokiDBConfig.DEFAULT_SEND_THREAD_METRICS_INTERVAL_TIME));
		config.put(LokiDBConfig.KEY_LOKI_LOG_ONLY_SAMPLER_RESPONSE_FAILED, Boolean.toString(false));
		config.put(LokiDBConfig.KEY_LOKI_BATCH_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_BATCH_TIMEOUT_MS));
		config.put(LokiDBConfig.KEY_CONNECTION_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_CONNECTION_TIMEOUT_MS));
		config.put(LokiDBConfig.KEY_REQUEST_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_REQUEST_TIMEOUT_MS));
		return config;
	}

	protected Map<String, String> lokiConfigWithExternalLabel() {
		Map<String, String> config = new HashMap<String, String>();
		config.put(LokiDBConfig.KEY_LOKI_DB_PROTOCOL, LokiDBConfig.DEFAULT_PROTOCOL);
		config.put(LokiDBConfig.KEY_LOKI_DB_HOST, LokiDBConfig.DEFAULT_HOST);
		config.put(LokiDBConfig.KEY_LOKI_DB_PORT, Integer.toString(LokiDBConfig.DEFAULT_PORT));
		config.put(LokiDBConfig.KEY_LOKI_DB_API_ENDPOINT, LokiDBConfig.DEFAUlT_LOKI_API_ENDPOINT);
		config.put(LokiDBConfig.KEY_LOKI_DB_BATCH_SIZE, Integer.toString(LokiDBConfig.DEFAULT_BATCH_SIZE));
		config.put(LokiDBConfig.KEY_LOKI_EXTERNAL_LABELS,
				"toilatester=external-labels,toi la tester with space=external-labels");
		config.put(LokiDBConfig.KEY_LOKI_DB_SEND_BATCH_INTERVAL_TIME,
				Long.toString(LokiDBConfig.DEFAULT_SEND_BATCH_INTERVAL_TIME));
		config.put(LokiDBConfig.KEY_LOKI_DB_SEND_THREAD_METRICS_INTERVAL_TIME,
				Long.toString(LokiDBConfig.DEFAULT_SEND_THREAD_METRICS_INTERVAL_TIME));
		config.put(LokiDBConfig.KEY_LOKI_LOG_ONLY_SAMPLER_RESPONSE_FAILED,
				Boolean.toString(LokiDBConfig.DEFAULT_LOG_RESPONSE_BODY_FAILED_SAMPLER_ONLY));
		config.put(LokiDBConfig.KEY_LOKI_BATCH_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_BATCH_TIMEOUT_MS));
		config.put(LokiDBConfig.KEY_CONNECTION_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_CONNECTION_TIMEOUT_MS));
		config.put(LokiDBConfig.KEY_REQUEST_TIMEOUT_MS, Long.toString(LokiDBConfig.DEFAULT_REQUEST_TIMEOUT_MS));
		return config;
	}

	protected <T> T deserializeJSONToObject(String json, Class<T> clzzz) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, clzzz);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
