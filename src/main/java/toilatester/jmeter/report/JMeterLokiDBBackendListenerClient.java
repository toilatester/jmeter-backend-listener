package toilatester.jmeter.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private boolean recordSubSamples;

	private static final Object LOCK = new Object();

	private static final int ONE_MS_IN_NANOSECONDS = 1000000;

	private ScheduledExecutorService sendLogDataScheduler;

	private ScheduledFuture<?> sendLogDataSchedulerSession;

	private ScheduledExecutorService addThreadMetricDataScheduler;

	private ScheduledFuture<?> addThreadMetricDataSchedulerSession;

	private BlockingQueue<LokiStreams> lokiStreamsQueue;

	private volatile int lokiStreamsCurrentQueueSize = 0;

	@SuppressWarnings("serial")
	private volatile Map<String, String> defaultLokiLabels = new HashMap<>() {
		{
			put("toilatester", "loki-plugin");
			put("jmeter_plugin", "loki-log");
		}
	};

	@Override
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		LOGGER.info("=============== Process =======================");

		List<SampleResult> allSampleResults = new ArrayList<>();
		collectAllSampleResult(allSampleResults, sampleResults);
		synchronized (LOCK) {
			storeSampleResultsToDB(allSampleResults);
		}
	}

	@Override
	public void setupTest(BackendListenerContext context) throws Exception {
		sendLogDataScheduler = Executors.newScheduledThreadPool(1,
				new LokiClientThreadFactory("send-loki-log-scheduler"));
		addThreadMetricDataScheduler = Executors.newScheduledThreadPool(1,
				new LokiClientThreadFactory("add-thread-metric-log-scheduler"));
		setUpLokiClient(context);
		addThreadMetricDataSchedulerSession = addThreadMetricDataScheduler.scheduleAtFixedRate(this, 1, 1,
				TimeUnit.SECONDS);
	}

	private void generateSetupTestLog() {
	};

	public void run() {
		ThreadCounts tc = JMeterContextService.getThreadCounts();
		String currentVirutalUsersLog = String.format(
				"minActiveThreads: %d \nmeanActiveThreads: %d \nmaxActiveThreads: %d \nstartedThreads: %d \nfinishedThreads: %d",
				getUserMetrics().getMinActiveThreads(), getUserMetrics().getMeanActiveThreads(),
				getUserMetrics().getMaxActiveThreads(), tc.startedThreads, tc.finishedThreads);
		generateThreadMetricLog(currentVirutalUsersLog);
	}

	private void generateThreadMetricLog(String log) {
		Map<String, String> labels = new HashMap<>();
		labels.put("jmeter_plugin", "toilatester");
		labels.put("external_label", "minhhoang");
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

	@Override
	public void teardownTest(BackendListenerContext context) throws Exception {
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

	private void setUpLokiClient(BackendListenerContext context) {
		lokiDBConfig = new LokiDBConfig(context);
		lokiClient = new LokiDBClient(lokiDBConfig, createlokiLogThreadPool(), createHttpClientThreadPool());
		sendLogDataSchedulerSession = sendLogDataScheduler.scheduleAtFixedRate(dispatchLogToLoki(), 500,
				lokiDBConfig.getLokiBatchIntervalTime(), TimeUnit.MILLISECONDS);
		this.defaultLokiLabels.putAll(lokiDBConfig.getLokiExternalLabels());
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

	private void storeSampleResultsToDB(List<SampleResult> allSampleResults) {
		generateSampleResultsToLokiStreamsObject(allSampleResults);
	}

	private Runnable dispatchLogToLoki() {
		return () -> LOGGER.info("================  Schedule Task To Send Loki Log ======================");
	}

	private void generateSampleResultsToLokiStreamsObject(List<SampleResult> allSampleResults) {
		Map<String, String> labels = new HashMap<>();
		labels.put("jmeter_plugin", "toilatester");
		labels.put("external_label", "minhhoang");
		LokiStreams lokiStreams = new LokiStreams();
		List<LokiStream> lokiStreamList = new ArrayList<>();
		for (List<SampleResult> partition : Lists.partition(allSampleResults, this.lokiDBConfig.getLokibBatchSize())) {
			List<List<String>> listLog = new ArrayList<>();
			LokiStream lokiStream = new LokiStream();
			lokiStream.setStream(labels);
			for (SampleResult result : partition) {
				listLog.add(new LokiLog("Sample log " + partition.indexOf(result) + " "
						+ Long.toString(System.currentTimeMillis() * 1000000)).getLogObject());
			}
			lokiStream.setValues(listLog);
			lokiStreamList.add(lokiStream);
		}
		lokiStreams.setStreams(lokiStreamList);
		this.lokiStreamsQueue.add(lokiStreams);
		this.lokiStreamsCurrentQueueSize += 1;
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
