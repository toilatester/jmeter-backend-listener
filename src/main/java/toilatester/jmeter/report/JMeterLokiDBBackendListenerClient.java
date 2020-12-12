package toilatester.jmeter.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

import toilatester.jmeter.config.loki.LokiClientThreadFactory;
import toilatester.jmeter.config.loki.LokiDBClient;
import toilatester.jmeter.config.loki.LokiDBConfig;
import toilatester.jmeter.config.loki.dto.LokiLog;
import toilatester.jmeter.config.loki.dto.LokiStream;
import toilatester.jmeter.config.loki.dto.LokiStreams;

public class JMeterLokiDBBackendListenerClient extends AbstractBackendListenerClient implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(JMeterLokiDBBackendListenerClient.class);

	private LokiDBClient lokiClient;

	private LokiDBConfig lokiDBConfig;

	private static final Object LOCK = new Object();

	private static final int ONE_MS_IN_NANOSECONDS = 1000000;

	private ScheduledExecutorService sendLogDataScheduler;

	private ScheduledFuture<?> sendLogDataSchedulerSession;

	private ScheduledExecutorService addThreadMetricDataScheduler;

	private ScheduledFuture<?> addThreadMetricDataSchedulerSession;

	private BlockingQueue<LokiStreams> lokiStreamsQueue = new LinkedBlockingDeque<LokiStreams>();;

	private volatile int lokiStreamsCurrentQueueSize = 0;

	private ObjectMapper mapper = new ObjectMapper();

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
			put("toilatester", "loki-plugin");
			put("jmeter_plugin", "loki-log");
		}
	};

	@Override
	public void setupTest(BackendListenerContext context) throws Exception {
		System.out.println("================  Set Up Test ======================");
		sendLogDataScheduler = Executors.newScheduledThreadPool(1,
				new LokiClientThreadFactory("send-loki-log-scheduler"));
		addThreadMetricDataScheduler = Executors.newScheduledThreadPool(1,
				new LokiClientThreadFactory("add-thread-metric-log-scheduler"));
		setUpLokiClient(context);
		addThreadMetricDataSchedulerSession = addThreadMetricDataScheduler.scheduleAtFixedRate(this, 1, 1,
				TimeUnit.SECONDS);
	}

	@Override
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		LOGGER.info("=============== Process =======================");
		System.out.println("================  Handler Sample Result ======================");
		System.out.println("Total sample results: " + sampleResults.size());
		List<SampleResult> allSampleResults = new ArrayList<>();
		collectAllSampleResult(allSampleResults, sampleResults);
		synchronized (LOCK) {
			storeSampleResultsToDB(allSampleResults);
		}
	}

	private void generateSetupTestLog() {
	};

	@Override
	public void run() {
		System.out.println("================  Schedule Task To Add Thread Metric To Loki Log ======================");
		ThreadCounts tc = JMeterContextService.getThreadCounts();
		generateThreadMetricLog(String.format(
				"[INFO] minActiveThreads: %d \nmeanActiveThreads: %d \nmaxActiveThreads: %d \nstartedThreads: %d \nfinishedThreads: %d",
				getUserMetrics().getMinActiveThreads(), getUserMetrics().getMeanActiveThreads(),
				getUserMetrics().getMaxActiveThreads(), tc.startedThreads, tc.finishedThreads));
	}

	@Override
	public void teardownTest(BackendListenerContext context) throws Exception {
		System.out.println("================  Teardown Test ======================");
		addThreadMetricDataSchedulerSession.cancel(true);
		addThreadMetricDataScheduler.shutdown();
		while (this.lokiStreamsCurrentQueueSize > 0) {
			System.out.println(String.format("Wait to complete send reamin %d ", this.lokiStreamsCurrentQueueSize));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}
		sendLogDataSchedulerSession.cancel(true);
		sendLogDataScheduler.shutdown();
		lokiClient.stopLokiClient(15, 15);
	}

	private void generateTearDownTestLog() {
	};

	private void generateThreadMetricLog(String log) {
		Map<String, String> labels = new HashMap<>();
		labels.put("jmeter_plugin_thread_metrics", "thread-metrics");
		labels.putAll(defaultLokiLabels);
		LokiStreams lokiStreams = new LokiStreams();
		List<LokiStream> lokiStreamList = new ArrayList<>();
		List<List<String>> listLog = new ArrayList<>();
		LokiStream lokiStream = new LokiStream();
		lokiStream.setStream(labels);
		listLog.add(new LokiLog(log).getLogObject());
		lokiStream.setValues(listLog);
		lokiStreamList.add(lokiStream);
		lokiStreams.setStreams(lokiStreamList);
		this.lokiStreamsQueue.add(lokiStreams);
		this.lokiStreamsCurrentQueueSize += 1;
	}

	@Override
	public Arguments getDefaultParameters() {
		Arguments arguments = new Arguments();
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_PROTOCOL, LokiDBConfig.DEFAULT_PROTOCOL);
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_HOST, LokiDBConfig.DEFAULT_HOST);
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_PORT, Integer.toString(LokiDBConfig.DEFAULT_PORT));
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_API_ENDPOINT, LokiDBConfig.DEFAUlT_LOKI_API_ENDPOINT);
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_BATCH_SIZE, Integer.toString(LokiDBConfig.DEFAULT_BATCH_SIZE));
		arguments.addArgument(LokiDBConfig.KEY_LOKI_DB_INTERVAL_TIME,
				Integer.toString(LokiDBConfig.DEFAULT_BATCH_INTERVAL_TIME));
		arguments.addArgument(LokiDBConfig.KEY_LOKI_EXTERNAL_LABELS, LokiDBConfig.DEFAUlT_LOKI_EXTERNAL_LABEL);
		arguments.addArgument(LokiDBConfig.KEY_LOKI_BATCH_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_BATCH_TIMEOUT_MS));
		arguments.addArgument(LokiDBConfig.KEY_CONNECTION_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_CONNECTION_TIMEOUT_MS));
		arguments.addArgument(LokiDBConfig.KEY_REQUEST_TIMEOUT_MS,
				Long.toString(LokiDBConfig.DEFAULT_REQUEST_TIMEOUT_MS));

		return arguments;
	}

	private void setUpLokiClient(BackendListenerContext context) {
		lokiDBConfig = new LokiDBConfig(context);
		lokiClient = new LokiDBClient(lokiDBConfig, createlokiLogThreadPool(), createHttpClientThreadPool());
		sendLogDataSchedulerSession = sendLogDataScheduler.scheduleAtFixedRate(dispatchLogToLoki(), 500, 1000,
				TimeUnit.MILLISECONDS);
		this.defaultLokiLabels.putAll(lokiDBConfig.getLokiExternalLabels());
		this.lokiReponseBodyLabels.putAll(defaultLokiLabels);
		this.lokiReponseHeaderLabels.putAll(defaultLokiLabels);
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
				subResult.setSampleLabel("Sub Request " + subResult.getSampleLabel());
				allSampleResults.add(subResult);
				getUserMetrics().add(sampleResult);
			}
		}
	}

	private void storeSampleResultsToDB(List<SampleResult> allSampleResults) {
		generateSampleResultsToLokiStreamsObject(allSampleResults);
	}

	private Runnable dispatchLogToLoki() {
		return () ->

		{
			System.out.println("================  Schedule Task To Send Loki Log ======================");
			try {
				String requestJSON = mapper.writeValueAsString(this.lokiStreamsQueue.poll());
				System.err.println(requestJSON);
				this.lokiClient.sendAsync(requestJSON.getBytes()).thenAccept(res -> {
					if (res.status != 204 && res.status != 200) {
						System.err.println(
								String.format("Error in send loki log to DB with status code [%d] and error [%s]",
										res.status, res.body));
					}
					System.err.println(res.status);
					System.err.println(res.body);
					this.lokiStreamsCurrentQueueSize--;
					LOGGER.info("================  Schedule Task To Send Loki Log ======================");
				});
			} catch (JsonProcessingException e) {
				System.err.println("Error in convert to JSON string " + e.getMessage());
				e.printStackTrace();
			}
		};
	}

	private void generateSampleResultsToLokiStreamsObject(List<SampleResult> allSampleResults) {
		LokiStreams lokiStreams = new LokiStreams();
		List<LokiStream> lokiStreamList = new ArrayList<>();
		for (List<SampleResult> partition : Lists.partition(allSampleResults, this.lokiDBConfig.getLokibBatchSize())) {
			List<List<String>> listLogResponseBody = new ArrayList<>();
			List<List<String>> listLogResponseHeader = new ArrayList<>();
			LokiStream lokiStreamResponseBody = new LokiStream();
			LokiStream lokiStreamResponseHeader = new LokiStream();
			lokiStreamResponseHeader.setStream(lokiReponseHeaderLabels);
			lokiStreamResponseBody.setStream(lokiReponseBodyLabels);
			for (SampleResult result : partition) {
				try {
					Map<String, SamplerMetric> metricsPerSampler = getMetricsPerSampler();
					metricsPerSampler.putIfAbsent(result.getSampleLabel(), new SamplerMetric());
					metricsPerSampler.get(result.getSampleLabel()).add(result);
				} catch (NullPointerException e) {
					System.err.println(result.getSampleLabel());
					System.err.println("Add sample result error " + e.getMessage());
				}
				listLogResponseBody.add(new LokiLog(generateResponseBodySampleLog(result)).getLogObject());
				listLogResponseHeader.add(new LokiLog(generateResponseHeaderSampleLog(result)).getLogObject());
			}
			lokiStreamResponseHeader.setValues(listLogResponseHeader);
			lokiStreamResponseBody.setValues(listLogResponseBody);
			lokiStreamList.add(lokiStreamResponseHeader);
			lokiStreamList.add(lokiStreamResponseBody);
		}
		lokiStreams.setStreams(lokiStreamList);
		this.lokiStreamsQueue.add(lokiStreams);
		this.lokiStreamsCurrentQueueSize += 1;
	}

	private String generateResponseBodySampleLog(SampleResult result) {
		return String.format("[INFO] Sample [%s]: \nHas status code: [%s] \nHas response body: \n%s",
				result.getSampleLabel(), result.getResponseCode(), result.getResponseDataAsString());
	}

	private String generateResponseHeaderSampleLog(SampleResult result) {
		return String.format("[INFO] Sample [%s]: \nHas response header: \nHas duration: %d",
				result.getResponseHeaders(), (result.getEndTime() - result.getStartTime()) / 1000);
	}

	private ExecutorService createHttpClientThreadPool() {
		return new ThreadPoolExecutor(0, Integer.MAX_VALUE, lokiDBConfig.getLokiBatchTimeout() * 10,
				TimeUnit.MILLISECONDS, // expire unused threads after 10 batch intervals
				new SynchronousQueue<Runnable>(), new LokiClientThreadFactory("jmeter-loki-java-http"));
	}

	private ExecutorService createlokiLogThreadPool() {
		return Executors.newFixedThreadPool(1, new LokiClientThreadFactory("jmeter-send-loki-log"));
	}

}
