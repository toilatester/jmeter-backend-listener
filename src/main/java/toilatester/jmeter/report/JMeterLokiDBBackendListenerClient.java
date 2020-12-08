package toilatester.jmeter.report;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toilatester.jmeter.config.loki.LokiClientThreadFactory;
import toilatester.jmeter.config.loki.LokiDBClient;
import toilatester.jmeter.config.loki.LokiDBConfig;

public class JMeterLokiDBBackendListenerClient extends AbstractBackendListenerClient implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(JMeterLokiDBBackendListenerClient.class);

	private LokiDBClient lokiClient;

	private LokiDBConfig lokiDBConfig;

	private boolean recordSubSamples;

	private static final Object LOCK = new Object();

	private static final int ONE_MS_IN_NANOSECONDS = 1000000;

	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> schedulerSession;

	@Override
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		LOGGER.info("=============== Process =======================");

		List<SampleResult> allSampleResults = new ArrayList<>();
		collectAllSampleResult(allSampleResults, sampleResults);
		synchronized (LOCK) {
			storeSampleResultToDB(allSampleResults);
		}
	}

	@Override
	public void setupTest(BackendListenerContext context) throws Exception {
		LOGGER.info("================  Set up JMeter Test Plan ...!! ======================");
		scheduler = Executors.newScheduledThreadPool(1, new LokiClientThreadFactory("loki-scheduler"));
		setUpLokiClient(context);
		String sampleLogBody = String.format(
				"{\"streams\":[{\"stream\":{\"jmeter_loki_plugin\":\"set-up-test\"},\"values\":[[\"%d\",\"This is sample log plugin when set up test\"]]}]}",
				System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS);
		lokiDBConfig.getLokiExternalLabels()
				.forEach((k, v) -> LOGGER.info(String.format("Key: %s \\nValue: %s", k, v)));
		LOGGER.info(sampleLogBody);
		lokiClient.sendAsync(sampleLogBody.getBytes()).whenComplete((r, e) -> {
			if (e != null) {
				LOGGER.info(e.getMessage());
			} else {
				LOGGER.info(String.format("loki response body %s", r.body));
				LOGGER.info(String.format("loki response code %d", r.status));
			}
		});

	}

	public void run() {
		LOGGER.info("============ Run Jmeter Test Plan ...!! ===================");
		try {
			String sampleLogBody = String.format(
					"{\"streams\":[{\"stream\":{\"jmeter_loki_plugin\":\"run-test\"},\"values\":[[\"%d\",\"This is sample log plugin when running test\"]]}]}",
					System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS);
			LOGGER.info(sampleLogBody);
			lokiClient.sendAsync(sampleLogBody.getBytes()).whenComplete((r, e) -> {
				if (e != null) {
					LOGGER.error(e.getMessage());
				} else {
					LOGGER.info(String.format("loki response body %s", r.body));
					LOGGER.info(String.format("loki response code %d", r.status));
				}
			});
		} catch (Exception e) {
			LOGGER.error("Failed writing to loki", e);
		}
	}

	@Override
	public Arguments getDefaultParameters() {
		Arguments arguments = new Arguments();
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_PROTOCOL, LokiDBConfig.DEFAULT_PROTOCOL);
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_HOST, LokiDBConfig.DEFAULT_HOST);
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_PORT, Integer.toString(LokiDBConfig.DEFAULT_PORT));
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_API_ENDPOINT, LokiDBConfig.DEFAUlT_LOKI_API_ENDPOINT);
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_BATCH_SIZE, Integer.toString(LokiDBConfig.DEFAULT_BATCH_SIZE));
		arguments.addArgument(LokiDBConfig.KEY_LOKI_EXTERNAL_LABELS, LokiDBConfig.DEFAUlT_LOKI_EXTERNAL_LABEL);
		arguments.addArgument(LokiDBConfig.KEY_LOKI_BATCH_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_BATCH_TIMEOUT_MS));
		arguments.addArgument(LokiDBConfig.KEY_CONNECTION_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_CONNECTION_TIMEOUT_MS));
		arguments.addArgument(LokiDBConfig.KEY_REQUEST_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_REQUEST_TIMEOUT_MS));

		return arguments;
	}

	@Override
	public void teardownTest(BackendListenerContext context) throws Exception {
		LOGGER.info("=============== Tear down JMeter Test Plan ...!! =================");
		lokiClient.stopLokiClient();
		schedulerSession.cancel(true);
		scheduler.shutdown();
	}

	private void setUpLokiClient(BackendListenerContext context) {
		LOGGER.info("================  Set up setUpLokiClient ...!! ======================");
		lokiDBConfig = new LokiDBConfig(context);
		lokiClient = new LokiDBClient(lokiDBConfig, createlokiLogThreadPool(), createHttpClientThreadPool());
		if (lokiClient != null)
			LOGGER.info("================  Set up setUpLokiClient completed ...!! ======================");
		schedulerSession = scheduler.scheduleAtFixedRate(
				() -> LOGGER.info("================  Schedule Task To Send Loki Log ======================"), 100,
				lokiDBConfig.getLokiBatchTimeout(), TimeUnit.MILLISECONDS);
	}

	private void collectAllSampleResult(List<SampleResult> allSampleResults, List<SampleResult> sampleResults) {
		for (SampleResult sampleResult : sampleResults) {
			String sampleName = sampleResult.getSampleLabel();
			boolean isTransactionSampleName = sampleName.toLowerCase().contains("transactions")
					|| sampleName.toLowerCase().contains("transaction");
			if (!isTransactionSampleName)
				sampleResult.setSampleLabel("Request " + sampleResult.getSampleLabel());
			allSampleResults.add(sampleResult);
			if (recordSubSamples) {
				for (SampleResult subResult : sampleResult.getSubResults()) {
					subResult.setSampleLabel("Sub Request " + subResult.getSampleLabel());
					allSampleResults.add(subResult);
				}
			}
		}
	}

	private void storeSampleResultToDB(List<SampleResult> allSampleResults) {
		for (SampleResult result : allSampleResults) {
			String sampleLogResponseBody = String.format(
					"{\"streams\":[{\"stream\":{\"jmeter_loki_plugin\":\"response-body\"},\"values\":[[\"%d\",\"%s\"]]}]}",
					System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS, result.getResponseDataAsString());
			String sampleLogHeader = String.format(
					"{\"streams\":[{\"stream\":{\"jmeter_loki_plugin\":\"response-header\"},\"values\":[[\"%d\",\"%s\"]]}]}",
					System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS, result.getResponseHeaders());
			lokiClient.sendAsync(sampleLogResponseBody.getBytes()).thenAccept(r -> {

			});
			lokiClient.sendAsync(sampleLogHeader.getBytes()).thenAccept(r -> {
			});
		}
	}

	private ExecutorService createHttpClientThreadPool() {
		return new ThreadPoolExecutor(0, Integer.MAX_VALUE, lokiDBConfig.getLokiBatchTimeout() * 10,
				TimeUnit.MILLISECONDS, // expire unused threads after 10 batch intervals
				new SynchronousQueue<Runnable>(), new LokiClientThreadFactory("jmeter-loki-java-http"));
	}

	private ExecutorService createlokiLogThreadPool() {
		return Executors.newFixedThreadPool(1, new LokiClientThreadFactory("jmeter-loki-log"));
	}

}
