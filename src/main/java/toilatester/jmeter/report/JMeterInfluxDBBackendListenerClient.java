package toilatester.jmeter.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import toilatester.jmeter.config.influxdb.InfluxDBConfig;
import toilatester.jmeter.config.influxdb.measurement.ConnectMeasurement;
import toilatester.jmeter.config.influxdb.measurement.ErrorMeasurement;
import toilatester.jmeter.config.influxdb.measurement.RequestMeasurement;
import toilatester.jmeter.config.influxdb.measurement.TestStartEndMeasurement;
import toilatester.jmeter.config.influxdb.measurement.VirtualUsersMeasurement;
import toilatester.jmeter.report.exception.ReportException;


/**
 * Backend listener that writes JMeter metrics to influxDB directly.
 * 
 * @author MinhHoang
 *
 */
public class JMeterInfluxDBBackendListenerClient extends AbstractBackendListenerClient implements Runnable {
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(JMeterInfluxDBBackendListenerClient.class);

	private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
	
	private static final String KEY_TEST_NAME = "testName";
	
	private static final String KEY_NODE_NAME = "nodeName";
	
	private static final String KEY_SAMPLERS_LIST = "samplersList";
	
	private static final String KEY_RECORD_SUB_SAMPLES = "recordSubSamples";

	private static final String SEPARATOR = ";";
	
	private static final int ONE_MS_IN_NANOSECONDS = 1000000;
	
	private static final Object LOCK = new Object();

	private ScheduledExecutorService scheduler;

	private String testName;

	private String nodeName;

	private String regexForSamplerList;

	private Set<String> samplersToFilter;

	InfluxDBConfig influxDBConfig;

	private InfluxDB influxDB;

	private Random randomNumberGenerator;

	private boolean recordSubSamples;


	@Override
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		// Gather all the listeners And Save Data
		List<SampleResult> allSampleResults = new ArrayList<>();
		collectAllSampleResult(allSampleResults, sampleResults);
		// Sending sample to db
		synchronized (LOCK) {
			storeSampleResultToDB(allSampleResults);
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
			if (recordSubSamples) {
				for (SampleResult subResult : sampleResult.getSubResults()) {
					subResult.setSampleLabel("Sub Request " + subResult.getSampleLabel());
					allSampleResults.add(subResult);
				}
			}
		}
	}

	private void storeSampleResultToDB(List<SampleResult> allSampleResults) {
		for (SampleResult sampleResult : allSampleResults) {
			getUserMetrics().add(sampleResult);
			boolean isMatchingRegexSample = null != regexForSamplerList
					&& sampleResult.getSampleLabel().matches(regexForSamplerList);
			boolean isMatchingFilterSample = samplersToFilter.contains(sampleResult.getSampleLabel());
			if (isMatchingRegexSample || isMatchingFilterSample) {
				createRequestsRawDataPoint(sampleResult);
				createErrorsRawDataPoint(sampleResult);
				createConnectRawDataPoint(sampleResult);
			}
		}
	}

	private void createRequestsRawDataPoint(SampleResult sampleResult) {
		Point point = Point.measurement(RequestMeasurement.MEASUREMENT_NAME)
				.time(System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS + getUniqueNumberForTheSamplerThread(),
						TimeUnit.NANOSECONDS)
				.tag(RequestMeasurement.Tags.REQUEST_NAME, sampleResult.getSampleLabel())
				.addField(RequestMeasurement.Fields.ERROR_COUNT, sampleResult.getErrorCount())
				.addField(RequestMeasurement.Fields.THREAD_NAME, sampleResult.getThreadName())
				.addField(RequestMeasurement.Fields.TEST_NAME, testName)
				.addField(RequestMeasurement.Fields.NODE_NAME, nodeName)
				.addField(RequestMeasurement.Fields.RESPONSE_TIME, sampleResult.getTime())
				.addField(RequestMeasurement.Fields.LATENCY_TIME, sampleResult.getLatency()).build();
		writeDataWithRetryInfluxDB(point);
	}

	private void createErrorsRawDataPoint(SampleResult sampleResult) {
		Point point = Point.measurement(ErrorMeasurement.MEASUREMENT_NAME)
				.time(System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS + getUniqueNumberForTheSamplerThread(),
						TimeUnit.NANOSECONDS)
				.tag(RequestMeasurement.Tags.REQUEST_NAME, sampleResult.getSampleLabel())
				.addField(RequestMeasurement.Fields.SAMPLE_NAME, sampleResult.getSampleLabel())
				.addField(ErrorMeasurement.Fields.RESPONSE_CODE, sampleResult.getResponseCode())
				.addField(ErrorMeasurement.Fields.ASSERT_DATA, getAssertMessage(sampleResult.getAssertionResults()))
				.build();
		writeDataWithRetryInfluxDB(point);
	}

	private void createConnectRawDataPoint(SampleResult sampleResult) {
		Point point = Point.measurement(ConnectMeasurement.MEASUREMENT_NAME)
				.time(System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS + getUniqueNumberForTheSamplerThread(),
						TimeUnit.NANOSECONDS)
				.tag(ConnectMeasurement.Tags.REQUEST_NAME, sampleResult.getSampleLabel())
				.addField(ConnectMeasurement.Fields.START_TIME, sampleResult.getStartTime())
				.addField(ConnectMeasurement.Fields.END_TIME, sampleResult.getEndTime())
				.addField(ConnectMeasurement.Fields.SAMPLE_NAME, sampleResult.getSampleLabel())
				.addField(ConnectMeasurement.Fields.RESPONSE_SIZE, sampleResult.getBodySizeAsLong())
				.addField(ConnectMeasurement.Fields.CONNECT_TIME, sampleResult.getConnectTime())
				.addField(ConnectMeasurement.Fields.SEND_BYTE, sampleResult.getSentBytes()).build();
		writeDataWithRetryInfluxDB(point);
	}

	private String getAssertMessage(AssertionResult[] result) {
		StringBuilder build = new StringBuilder();
		for (AssertionResult rs : result) {
			build.append(rs.getName() + ": " + rs.getFailureMessage());
		}
		return build.toString();
	}

	private void writeDataWithRetryInfluxDB(Point point) {
		try {
			influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), point);
		} catch (RuntimeException e) {
			try {
				influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), point);
			} catch (RuntimeException retry) {
				throw new ReportException("Has Error In Stored Sample To DB. ", retry);
			}
		}
	}

	@Override
	public Arguments getDefaultParameters() {
		Arguments arguments = new Arguments();
		arguments.addArgument(KEY_TEST_NAME, "Test");
		arguments.addArgument(KEY_NODE_NAME, "Test-Node");
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PROTOCOL, InfluxDBConfig.DEFAULT_PROTOCOL);
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_HOST, InfluxDBConfig.DEFAULT_HOST);
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PORT, Integer.toString(InfluxDBConfig.DEFAULT_PORT));
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_USER, "");
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PASSWORD, "");
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_DATABASE, InfluxDBConfig.DEFAULT_DATABASE);
		arguments.addArgument(InfluxDBConfig.KEY_RETENTION_POLICY, InfluxDBConfig.DEFAULT_RETENTION_POLICY);
		arguments.addArgument(KEY_SAMPLERS_LIST, ".*");
		arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, "true");
		arguments.addArgument(KEY_RECORD_SUB_SAMPLES, "true");
		return arguments;
	}

	@Override
	public void setupTest(BackendListenerContext context) throws Exception {
		super.setupTest(context);
		testName = context.getParameter(KEY_TEST_NAME, "Test");
		randomNumberGenerator = new Random();
		nodeName = context.getParameter(KEY_NODE_NAME, "Test-Node");

		setupInfluxClient(context);
		influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(),
				Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME)
						.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
						.tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.STARTED)
						.tag(TestStartEndMeasurement.Tags.NODE_NAME, nodeName)
						.addField(TestStartEndMeasurement.Fields.TEST_NAME, testName).build());

		parseSamplers(context);
		scheduler = Executors.newScheduledThreadPool(1);

		scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);

		// Indicates whether to write sub sample records to the database
		recordSubSamples = Boolean.parseBoolean(context.getParameter(KEY_RECORD_SUB_SAMPLES, "false"));
	}

	@Override
	public void teardownTest(BackendListenerContext context) throws Exception {
		LOGGER.info("Shutting down influxDB scheduler...");
		scheduler.shutdown();

		addVirtualUsersMetrics(0, 0, 0, 0, JMeterContextService.getThreadCounts().finishedThreads);
		influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(),
				Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME)
						.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
						.tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.FINISHED)
						.tag(TestStartEndMeasurement.Tags.NODE_NAME, nodeName)
						.addField(TestStartEndMeasurement.Fields.TEST_NAME, testName).build());

		influxDB.disableBatch();
		try {
			scheduler.awaitTermination(60, TimeUnit.SECONDS);
			LOGGER.info("influxDB scheduler terminated!");
		} catch (InterruptedException e) {
			LOGGER.error("Error waiting for end of scheduler");
		}

		samplersToFilter.clear();
		super.teardownTest(context);
	}

	/**
	 * Periodically writes virtual users metrics to influxDB.
	 */
	public void run() {
		try {
			ThreadCounts tc = JMeterContextService.getThreadCounts();
			addVirtualUsersMetrics(getUserMetrics().getMinActiveThreads(), getUserMetrics().getMeanActiveThreads(),
					getUserMetrics().getMaxActiveThreads(), tc.startedThreads, tc.finishedThreads);
		} catch (Exception e) {
			LOGGER.error("Failed writing to influx", e);
		}
	}

	/**
	 * Setup influxDB client.
	 * 
	 * @param context
	 *            {@link BackendListenerContext}.
	 */
	private void setupInfluxClient(BackendListenerContext context) {
		influxDBConfig = new InfluxDBConfig(context);
		OkHttpClient.Builder build = new OkHttpClient().newBuilder().readTimeout(60, TimeUnit.SECONDS)
				.connectTimeout(60, TimeUnit.SECONDS);
		createInfluxDBConnection(build);		
		influxDB.enableBatch(100, 15, TimeUnit.SECONDS);
		createDatabaseIfNotExistent();
	}
	
	private void createInfluxDBConnection(OkHttpClient.Builder build){
		try{
			influxDB = InfluxDBFactory.connect(influxDBConfig.getInfluxDBURL(), influxDBConfig.getInfluxUser(),
					influxDBConfig.getInfluxPassword(), build);
			}
		catch(IllegalArgumentException e){
			influxDB = InfluxDBFactory.connect(influxDBConfig.getInfluxDBURL(), "default",
					"default", build);
		}
	}
	/**
	 * Parses list of samplers.
	 * 
	 * @param context
	 *            {@link BackendListenerContext}.
	 */
	private void parseSamplers(BackendListenerContext context) {
		String samplersList = context.getParameter(KEY_SAMPLERS_LIST, "");
		samplersToFilter = new HashSet<>();
		if (context.getBooleanParameter(KEY_USE_REGEX_FOR_SAMPLER_LIST, false)) {
			regexForSamplerList = samplersList;
		} else {
			regexForSamplerList = null;
			String[] samplers = samplersList.split(SEPARATOR);
			samplersToFilter = new HashSet<>();
			for (String samplerName : samplers) {
				samplersToFilter.add(samplerName);
			}
		}
	}

	/**
	 * Write thread metrics.
	 */
	private void addVirtualUsersMetrics(int minActiveThreads, int meanActiveThreads, int maxActiveThreads,
			int startedThreads, int finishedThreads) {
		Builder builder = Point.measurement(VirtualUsersMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(),
				TimeUnit.MILLISECONDS);
		builder.addField(VirtualUsersMeasurement.Fields.MIN_ACTIVE_THREADS, minActiveThreads);
		builder.addField(VirtualUsersMeasurement.Fields.MAX_ACTIVE_THREADS, maxActiveThreads);
		builder.addField(VirtualUsersMeasurement.Fields.MEAN_ACTIVE_THREADS, meanActiveThreads);
		builder.addField(VirtualUsersMeasurement.Fields.STARTED_THREADS, startedThreads);
		builder.addField(VirtualUsersMeasurement.Fields.FINISHED_THREADS, finishedThreads);
		builder.tag(VirtualUsersMeasurement.Tags.NODE_NAME, nodeName);
		influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), builder.build());
	}

	/**
	 * Creates the configured database in influx if it does not exist yet.
	 */
	private void createDatabaseIfNotExistent() {
		List<String> dbNames = influxDB.describeDatabases();
		if (!dbNames.contains(influxDBConfig.getInfluxDatabase())) {
			influxDB.createDatabase(influxDBConfig.getInfluxDatabase());
		}
	}

	/**
	 * Try to get a unique number for the sampler thread
	 */
	private int getUniqueNumberForTheSamplerThread() {
		return randomNumberGenerator.nextInt(ONE_MS_IN_NANOSECONDS);
	}

}
