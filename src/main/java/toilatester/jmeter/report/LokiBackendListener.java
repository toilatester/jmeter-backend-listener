package toilatester.jmeter.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.apache.jmeter.visualizers.backend.SamplerMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import toilatester.jmeter.config.loki.LogLevel;
import toilatester.jmeter.config.loki.LokiClientThreadFactory;
import toilatester.jmeter.config.loki.LokiDBClient;
import toilatester.jmeter.config.loki.LokiDBConfig;
import toilatester.jmeter.config.loki.dto.LokiLog;
import toilatester.jmeter.config.loki.dto.LokiResponse;
import toilatester.jmeter.config.loki.dto.LokiStream;
import toilatester.jmeter.config.loki.dto.LokiStreams;

public class LokiBackendListener extends AbstractBackendListenerClient implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(LokiBackendListener.class);

	private static final Object LOCK = new Object();

	private LokiDBClient lokiClient;

	private LokiDBConfig lokiDBConfig;

	private ScheduledExecutorService sendLogDataScheduler;

	private ScheduledFuture<?> sendLogDataSchedulerSession;

	private ScheduledExecutorService addThreadMetricDataScheduler;

	private ScheduledFuture<?> addThreadMetricDataSchedulerSession;

	private LinkedBlockingQueue<LokiLog> lokiResponseDataLogQueue = new LinkedBlockingQueue<LokiLog>();

	private static int lokiStreamsCurrentQueueSize = 0;

	private ObjectMapper mapper = new ObjectMapper();

	private static int totalSendLog = 0;

	private static int totalLogResponse = 0;

	private static int totalLogAddQueue = 0;

	private static int totalLogConvertLokiStreams = 0;

	@SuppressWarnings("serial")
	private Map<String, String> lokiReponseHeaderLabels = new HashMap<>() {
		{
			put("jmeter_data", "response-header");
		}
	};

	@SuppressWarnings("serial")
	private Map<String, String> lokiReponseBodyLabels = new HashMap<>() {
		{
			put("jmeter_data", "response-body");
		}
	};

	@SuppressWarnings("serial")
	private Map<String, String> defaultLokiLabels = new HashMap<>() {
		{
			put("jmeter_plugin", "loki-log");
		}
	};

	@Override
	public void setupTest(BackendListenerContext context) throws Exception {
		super.setupTest(context);
		try {
			LOGGER.info("Set up running Loki plugin");
			sendLogDataScheduler = Executors.newScheduledThreadPool(1,
					new LokiClientThreadFactory("send-loki-log-scheduler"));
			addThreadMetricDataScheduler = Executors.newScheduledThreadPool(1,
					new LokiClientThreadFactory("add-thread-metric-log-scheduler"));
			setUpLokiClient(context);
			sendLogDataSchedulerSession = sendLogDataScheduler.scheduleAtFixedRate(dispatchResponseLogToLoki(), 500,
					this.lokiDBConfig.getLokiSendBatchIntervalTime(), TimeUnit.MILLISECONDS);
			addThreadMetricDataSchedulerSession = addThreadMetricDataScheduler.scheduleAtFixedRate(this, 500,
					this.lokiDBConfig.getLokiSendBatchIntervalTime(), TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			LOGGER.error(String.format("Error in set up test %s", e.getMessage()));
			if (sendLogDataSchedulerSession != null)
				sendLogDataSchedulerSession.cancel(true);
			sendLogDataScheduler.shutdown();
			if (addThreadMetricDataSchedulerSession != null)
				addThreadMetricDataSchedulerSession.cancel(true);
			addThreadMetricDataScheduler.shutdown();
			JMeterContextService.endTest();
			throw e;
		}
	}

	private void setUpLokiClient(BackendListenerContext context) {
		lokiDBConfig = new LokiDBConfig(context);
		lokiClient = new LokiDBClient(lokiDBConfig, createlokiLogThreadPool(), createHttpClientThreadPool());
		this.defaultLokiLabels.putAll(lokiDBConfig.getLokiExternalLabels());
		this.lokiReponseBodyLabels.putAll(defaultLokiLabels);
		this.lokiReponseHeaderLabels.putAll(defaultLokiLabels);
	}

	@Override
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		LOGGER.info("Store sampler results to loki database");
		List<SampleResult> allSampleResults = new ArrayList<>();
		collectAllSampleResult(allSampleResults, sampleResults);
		synchronized (LOCK) {
			storeSampleResultsToDB(allSampleResults);
		}
	}

	private void collectAllSampleResult(List<SampleResult> allSampleResults, List<SampleResult> sampleResults) {
		for (SampleResult sampleResult : sampleResults) {
			String sampleName = sampleResult.getSampleLabel();
			boolean isTransactionSampleName = sampleName.toLowerCase().contains("transactions")
					|| sampleName.toLowerCase().contains("transaction");
			if (!isTransactionSampleName)
				sampleResult.setSampleLabel("Request " + sampleResult.getSampleLabel());
			allSampleResults.add(sampleResult);
			getUserMetrics().add(sampleResult);
			for (SampleResult subResult : sampleResult.getSubResults()) {
				subResult.setSampleLabel("Request " + subResult.getSampleLabel());
				allSampleResults.add(subResult);
				getUserMetrics().add(sampleResult);
			}
		}
	}

	private void storeSampleResultsToDB(List<SampleResult> allSampleResults) {
		for (SampleResult result : allSampleResults) {
			addSamplerMetric(result);
			boolean putToQueueSuccess = false;
			int maxRetry = 3;
			while (maxRetry > 0 && !putToQueueSuccess) {
				try {
//					this.lokiResponseDataLogQueue.put(
//							new LokiLog(result.getErrorCount() == 0 ? LogLevel.INFO.value() : LogLevel.ERROR.value(),
//									generateResponseDataLog(result)));
					this.lokiResponseDataLogQueue.put(
							new LokiLog(result.getErrorCount() == 0 ? LogLevel.INFO.value() : LogLevel.ERROR.value(),
									"Count " + totalLogAddQueue));
					totalLogAddQueue += 1;
					putToQueueSuccess = true;
				} catch (InterruptedException e) {
					waitForQueueHasSpace();
					maxRetry -= 1;
				}
			}
			totalLogResponse += 1;
		}

	}

	private void addSamplerMetric(SampleResult result) {
		try {
			Map<String, SamplerMetric> metricsPerSampler = getMetricsPerSampler();
			metricsPerSampler.putIfAbsent(result.getSampleLabel(), new SamplerMetric());
			metricsPerSampler.get(result.getSampleLabel()).add(result);
		} catch (NullPointerException e) {
			LOGGER.error(
					String.format("Error in add sampler [%s] metrics. %s", result.getSampleLabel(), e.getMessage()));
		}
	}

	private void waitForQueueHasSpace() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			LOGGER.info("Current queue size after wait 1000ms is " + this.lokiResponseDataLogQueue.size());
		}
	}

	private String generateResponseDataLog(SampleResult result) {
		return String.format("Sampler [%s] \n\n%s \n\n%s", result.getSampleLabel(),
				this.generateResponseHeaderSampleLog(result), this.generateResponseBodySampleLog(result));
	}

	private String generateResponseHeaderSampleLog(SampleResult result) {
		return String.format("Response Header: %s \nRequest Duration: %d ms", result.getResponseHeaders(),
				result.getTime());
	}

	private String generateResponseBodySampleLog(SampleResult result) {
		String responseBody = "Turn on log reponse body for failure sampler only";
		boolean logResponseBodyDataWithTurnOnLogFailureMode = this.lokiDBConfig.isLokiLogResponseBodyFailedSamplerOnly()
				&& result.getErrorCount() > 0;
		if (logResponseBodyDataWithTurnOnLogFailureMode
				|| !this.lokiDBConfig.isLokiLogResponseBodyFailedSamplerOnly()) {
			responseBody = result.getResponseDataAsString();
		}
		StringBuilder assertionResultMsg = new StringBuilder();
		Arrays.asList(result.getAssertionResults()).forEach(assertionResult -> {
			assertionResultMsg.append(
					String.format("Assertion Name [%s] - Is Error [%b] - Is Failure [%b] \nFailure Message: %s\n",
							assertionResult.getName(), assertionResult.isError(), assertionResult.isFailure(),
							assertionResult.getFailureMessage()));
		});
		return String.format("Assertions Result: \n%s \nResponse Body: \n%s", assertionResultMsg.toString(),
				responseBody);
	}

	@Override
	public void run() {
		LOGGER.info("Store thread metrics to loki database");
		this.sendThreadMetricsLog();
	}

	private void sendThreadMetricsLog() {
		try {
			LokiStreams lokiStreams = this.generateThreadMetricLog();
			String requestJSON = mapper.writeValueAsString(lokiStreams);
			LOGGER.debug("Loki Request Payload: " + requestJSON);
			this.lokiClient.sendAsync(requestJSON.getBytes()).thenAccept(this.lokiThreadMetricsResponseHandler);
		} catch (JsonProcessingException e) {
			LOGGER.error("Error JSON Convert: " + e.getMessage());
			this.lokiClient
					.sendAsync(String.format("[Error] Can't generate test metrics %s", e.getMessage()).getBytes())
					.thenAccept(this.lokiThreadMetricsResponseHandler);
		}
	}

	private LokiStreams generateThreadMetricLog() {
		ThreadCounts tc = JMeterContextService.getThreadCounts();
		Map<String, String> labels = new HashMap<>();
		labels.put("jmeter_plugin_metrics", "thread-metrics");
		labels.putAll(defaultLokiLabels);
		LokiStreams lokiStreams = new LokiStreams();
		List<LokiStream> lokiStreamList = new ArrayList<>();
		List<List<String>> listLog = new ArrayList<>();
		LokiStream lokiStream = new LokiStream();
		lokiStream.setStream(labels);
		listLog.add(new LokiLog(LogLevel.INFO.value(), String.format(
				"minActiveThreads: %d \nmeanActiveThreads: %d \nmaxActiveThreads: %d \nstartedThreads: %d \nfinishedThreads: %d",
				getUserMetrics().getMinActiveThreads(), getUserMetrics().getMeanActiveThreads(),
				getUserMetrics().getMaxActiveThreads(), tc.startedThreads, tc.finishedThreads)).getLogObject());
		lokiStream.setValues(listLog);
		lokiStreamList.add(lokiStream);
		lokiStreams.setStreams(lokiStreamList);
		return lokiStreams;
	}

	@Override
	public void teardownTest(BackendListenerContext context) throws Exception {
		LOGGER.info("Stop running Loki plugin");
		addThreadMetricDataSchedulerSession.cancel(true);
		addThreadMetricDataScheduler.shutdown();
		while (lokiResponseDataLogQueue.size() > 0) {
			waitForSendingAllLogCompleted(2000);
		}
		sendLogDataSchedulerSession.cancel(true);
		sendLogDataScheduler.shutdown();
		this.sendAllSamplersMetricsLog().thenAccept((res) -> {
			lokiClient.stopLokiClient(15, 15);
		});
		LOGGER.info("============ Total Log Sending " + totalSendLog);
		LOGGER.info("============ Total Log Adding " + totalLogResponse);
		LOGGER.info("============ Total Log Adding Queue " + totalLogAddQueue);
		LOGGER.info("============ Total Log Convert To LokiStreams " + totalLogConvertLokiStreams);
		totalSendLog = 0;
		totalLogResponse = 0;
		totalLogAddQueue = 0;
		totalLogConvertLokiStreams = 0;
		super.teardownTest(context);
	}

	private CompletableFuture<LokiResponse> sendAllSamplersMetricsLog() {
		try {
			Map<String, String> labels = new HashMap<>();
			labels.put("jmeter_plugin_metrics", "sampler-metrics");
			labels.putAll(defaultLokiLabels);
			LokiStreams lokiStreams = new LokiStreams();
			List<LokiStream> lokiStreamList = new ArrayList<>();
			LokiStream lokiStream = new LokiStream();
			lokiStream.setStream(labels);
			lokiStream.setValues(generateTearDownTestLog());
			lokiStreamList.add(lokiStream);
			lokiStreams.setStreams(lokiStreamList);
			String requestJSON = mapper.writeValueAsString(lokiStreams);
			return this.lokiClient.sendAsync(requestJSON.getBytes());
		} catch (JsonProcessingException e) {
			LOGGER.error("Error JSON Convert: " + e.getMessage());
			return this.lokiClient
					.sendAsync(String.format("[Error] Can't generate test metrics %s", e.getMessage()).getBytes());
		}
	}

	private List<List<String>> generateTearDownTestLog() {
		List<List<String>> sampleMetricsLogs = new ArrayList<>();
		getMetricsPerSampler().entrySet().forEach(e -> {
			sampleMetricsLogs.add(new LokiLog(LogLevel.INFO.value(),
					String.format("Sample Name [%s]: \n\n%s", e.getKey(), this.generateSampleMetricsLog(e.getValue())))
							.getLogObject());
		});
		return sampleMetricsLogs;
	};

	private String generateSampleMetricsLog(SamplerMetric sampleMetric) {
		return String.format("%s \n%s \n%s", generateAllSampleRequestsMetricLog(sampleMetric),
				generateAllOkSampleRequestsMetricLog(sampleMetric), generateAllKoSampleRequestsMetricLog(sampleMetric));
	}

	private String generateAllSampleRequestsMetricLog(SamplerMetric sampleMetric) {
		return String.format(
				"All Min Time: %f \nAll Mean Time: %f \nAll Max Time: %f \nTotal Failures: %d \nTotal Hits: %d \nTotal Requests: %d \nTotal Successes Requests: %d \n50th: %f \n70th %f \n90th %f \n95th %f\n",
				sampleMetric.getAllMinTime(), sampleMetric.getAllMean(), sampleMetric.getAllMaxTime(),
				sampleMetric.getFailures(), sampleMetric.getHits(), sampleMetric.getTotal(),
				sampleMetric.getSuccesses(), sampleMetric.getAllPercentile(50.0), sampleMetric.getAllPercentile(70.0),
				sampleMetric.getAllPercentile(90.0), sampleMetric.getAllPercentile(95.0));

	}

	private String generateAllOkSampleRequestsMetricLog(SamplerMetric sampleMetric) {
		return String.format(
				"All Ok Min Time: %f \nAll Ok Mean Time: %f \nAll Ok Max Time: %f \n50th: %f \n70th %f \n90th %f \n95th %f\n",
				sampleMetric.getOkMinTime(), sampleMetric.getOkMean(), sampleMetric.getOkMaxTime(),
				sampleMetric.getOkPercentile(50.0), sampleMetric.getOkPercentile(70.0),
				sampleMetric.getOkPercentile(90.0), sampleMetric.getOkPercentile(95.0));
	}

	private String generateAllKoSampleRequestsMetricLog(SamplerMetric sampleMetric) {
		return String.format(
				"All Ko Min Time: %f \nAll Ko Mean Time: %f \nAll Ko Max Time: %f \n50th: %f \n70th %f \n90th %f \n95th %f\n",
				sampleMetric.getKoMinTime(), sampleMetric.getKoMean(), sampleMetric.getKoMaxTime(),
				sampleMetric.getKoPercentile(50.0), sampleMetric.getKoPercentile(70.0),
				sampleMetric.getKoPercentile(90.0), sampleMetric.getKoPercentile(95.0));
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
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_SEND_BATCH_INTERVAL_TIME,
				Integer.toString(LokiDBConfig.DEFAULT_SEND_BATCH_INTERVAL_TIME));
		arguments.addArgument(LokiDBConfig.KEY_LOKI_LOG_ONLY_SAMPLER_RESPONSE_FAILED,
				Boolean.toString(LokiDBConfig.DEFAULT_LOG_RESPONSE_BODY_FAILED_SAMPLER_ONLY));
		arguments.addArgument(LokiDBConfig.KEY_LOKI_BATCH_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_BATCH_TIMEOUT_MS));
		arguments.addArgument(LokiDBConfig.KEY_CONNECTION_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_CONNECTION_TIMEOUT_MS));
		arguments.addArgument(LokiDBConfig.KEY_REQUEST_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_REQUEST_TIMEOUT_MS));
		return arguments;
	}

	private Runnable dispatchResponseLogToLoki() {
		return () -> {
			Map<String, String> labels = new HashMap<>();
			labels.put("jmeter_data", "response-data");
			labels.putAll(defaultLokiLabels);
			List<List<String>> listLog = new ArrayList<>();
			while (true) {
				LokiLog lokiLog = this.lokiResponseDataLogQueue.poll();
				if (lokiLog == null)
					break;
				listLog.add(lokiLog.getLogObject());
			}
			if (listLog.size() == 0)
				return;
			this.sendLog(listLog, labels);

		};
	}

	private void sendLog(List<List<String>> listLog, Map<String, String> labels) {
		try {
			LOGGER.info("===== Total Logs Adding " + listLog.size());
			totalLogConvertLokiStreams += listLog.size();
			for (List<List<String>> listLogPartition : Lists.partition(listLog,
					this.lokiDBConfig.getLokibBatchSize())) {
				LokiStreams lokiStreams = new LokiStreams();
				List<LokiStream> lokiStreamList = new ArrayList<>();
				LokiStream lokiStream = new LokiStream();
				lokiStream.setStream(labels);
				lokiStream.setValues(listLogPartition);
				lokiStreamList.add(lokiStream);
				lokiStreams.setStreams(lokiStreamList);
				String requestJSON = mapper.writeValueAsString(lokiStreams);
				LOGGER.info("Loki Request Payload: " + requestJSON);
				lokiStreamsCurrentQueueSize += 1;
				this.lokiClient.sendAsync(requestJSON.getBytes()).thenAccept(this.lokiReponseDataResponseHandler);
			}
		} catch (JsonProcessingException e) {
			LOGGER.error("Error JSON Convert: " + e.getMessage());
			this.lokiClient
					.sendAsync(String.format("[Error] Can't generate test metrics %s", e.getMessage()).getBytes())
					.thenAccept(this.lokiReponseDataResponseHandler);
		}
	}

	private ExecutorService createHttpClientThreadPool() {
		return new ThreadPoolExecutor(0, Integer.MAX_VALUE, lokiDBConfig.getLokiBatchTimeout() * 10,
				TimeUnit.MILLISECONDS, // expire unused threads after 10 batch intervals
				new SynchronousQueue<Runnable>(), new LokiClientThreadFactory("jmeter-loki-java-http"));
	}

	private ExecutorService createlokiLogThreadPool() {
		return Executors.newFixedThreadPool(1, new LokiClientThreadFactory("jmeter-send-loki-log"));
	}

	private Consumer<LokiResponse> lokiReponseDataResponseHandler = (response) -> {
		LOGGER.debug("============= Send Log To DB ===================");
		LOGGER.debug(Integer.toString(response.status));
		LOGGER.debug(response.body);
		if (response.status != 204 && response.status != 200) {
			LOGGER.error(String.format("Error in send loki log to DB with status code [%d] and error [%s]",
					response.status, response.body));
		}
		lokiStreamsCurrentQueueSize -= 1;
		totalSendLog += 1;
	};

	private Consumer<LokiResponse> lokiThreadMetricsResponseHandler = (response) -> {
		LOGGER.debug("============= Send Log To DB ===================");
		LOGGER.debug(Integer.toString(response.status));
		LOGGER.debug(response.body);
		if (response.status != 204 && response.status != 200) {
			LOGGER.error(String.format("Error in send loki log to DB with status code [%d] and error [%s]",
					response.status, response.body));
		}
		totalSendLog += 1;
	};

	private void waitForSendingAllLogCompleted(long timeOut) {
		try {
			Thread.sleep(timeOut);
			LOGGER.info(String.format("Wait to complete send reamin %d ", lokiStreamsCurrentQueueSize));
		} catch (InterruptedException e) {
			LOGGER.info(String.format("Wait to complete send reamin %d ", lokiStreamsCurrentQueueSize));
		}
	}
}
