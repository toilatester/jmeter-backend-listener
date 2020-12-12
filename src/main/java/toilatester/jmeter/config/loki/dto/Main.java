package toilatester.jmeter.config.loki.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import toilatester.jmeter.config.loki.LokiClientThreadFactory;
import toilatester.jmeter.config.loki.LokiDBClient;

public class Main {
	private static LokiDBClient lokiClient;
	private static int generateLogCount = 0;
	private static int sendLogCount = 0;
	private static int totalLokiStream = 0;
	private static final int MAX_STREAM_PER_REQUEST = 256;
	private static String LOCAL_URL = "http://localhost:3100/loki/api/v1/push";
	private static BlockingQueue<LokiStreams> queue;
	private static ScheduledExecutorService addScheduler = Executors.newScheduledThreadPool(1,
			new LokiClientThreadFactory("loki-scheduler-add"));;
	private static ScheduledFuture<?> addSchedulerSession;
	private static ScheduledExecutorService sendScheduler = Executors.newScheduledThreadPool(1,
			new LokiClientThreadFactory("loki-scheduler-send"));;
	private static ScheduledFuture<?> sendSchedulerSession;

	private static class AddLokiLog {
		private Queue<LokiStreams> queue;

		public AddLokiLog(Queue<LokiStreams> queue) {
			this.queue = queue;
		}

		public void addLog() {
			Map<String, String> labels = new HashMap<>();
			labels.put("jmeter_plugin", "toilatester");
			labels.put("external_label", "minhhoang");
			LokiStreams lokiStreams = new LokiStreams();
			LokiStream lokiStream = new LokiStream();
			int totalRandomLog = new Random().nextInt(5000);
			generateLogCount += totalRandomLog;
			System.err.println(String.format("Total Generate Log %d", totalRandomLog));
			List<List<String>> listLog = new ArrayList<>();
			List<LokiStream> lokiStreamList = new ArrayList<>();
			for (int i = 0; i < totalRandomLog; i++) {
				if (i % MAX_STREAM_PER_REQUEST == 0) {
					listLog = new ArrayList<>();
					lokiStream = new LokiStream();
					lokiStream.setStream(labels);
					lokiStream.setValues(listLog);
					lokiStreamList.add(lokiStream);

				}
				listLog.add(new LokiLog("Sample log " + i + " " + Long.toString(System.currentTimeMillis() * 1000000))
						.getLogObject());
			}
			lokiStreams.setStreams(lokiStreamList);
			this.queue.add(lokiStreams);
			sendLogCount += 1;
			totalLokiStream += lokiStreamList.size();
		}
	}

	/**
	 * Just for debug and verify solution :(
	 * 
	 * @param args
	 */

	private static ExecutorService createHttpClientThreadPool() {
		return new ThreadPoolExecutor(5, Integer.MAX_VALUE, 5000 * 10, TimeUnit.MILLISECONDS, // expire unused threads
																								// after 10 batch
																								// intervals
				new SynchronousQueue<Runnable>(), new LokiClientThreadFactory("jmeter-loki-java-http"));
	}

	private static ExecutorService createlokiLogThreadPool() {
		return Executors.newFixedThreadPool(1, new LokiClientThreadFactory("jmeter-loki-log"));
	}

	private static void sendLog() {
		ObjectMapper mapper = new ObjectMapper();
		System.err.println(String.format("Total log to send %d", queue.size()));
		int queueLength = queue.size();
		while (queueLength > 0) {

			try {
				String requestJSON = mapper.writeValueAsString(queue.poll());
				lokiClient.sendAsync(requestJSON.getBytes()).thenAccept(response -> {
					sendLogCount--;
					if (response.status != 204) {
						System.err.println(response.status);
						System.err.println(response.body);
					}
					System.out.println(response.status);
				});

			} catch (Exception e) {
				e.printStackTrace();
			}
			queueLength = queue.size();
		}
	}

	public void main(String[] args) {
		System.err.println("===============================");
		System.err.println("===============================");
		System.err.println("===============================");
		lokiClient = new LokiDBClient(createlokiLogThreadPool(), createHttpClientThreadPool());
		lokiClient.createLokiClient(LOCAL_URL, 5000, 5000);
		Instant start = Instant.now();
		Instant stop = Instant.now();
		Duration limit = Duration.ofSeconds(15);
		Duration duration = Duration.between(start, stop);
		queue = new LinkedBlockingDeque<LokiStreams>();
		AddLokiLog addLogScheduler = new AddLokiLog(queue);
		addSchedulerSession = addScheduler.scheduleAtFixedRate(() -> addLogScheduler.addLog(), 500, 1000,
				TimeUnit.MILLISECONDS);
		sendSchedulerSession = sendScheduler.scheduleAtFixedRate(() -> sendLog(), 500, 1500, TimeUnit.MILLISECONDS);
		while ((duration.compareTo(limit) <= 0)) {
			duration = Duration.between(start, Instant.now());
		}
		addSchedulerSession.cancel(true);
		addScheduler.shutdown();
		System.err.println(String.format("Current queue size after shutdown add: %d", queue.size()));
		int remainLog = queue.size();
		while (remainLog > 0) {
			remainLog = queue.size();
		}
		System.err.println("Completed send all log!!!!!!!!!!!!!!!!!!!!!!!!");
		System.err.println("Remain log to send " + sendLogCount);
		while (sendLogCount > 0) {
			System.out.println(String.format("Wait to complete send reamin %d ", sendLogCount));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}
		sendSchedulerSession.cancel(true);
		sendScheduler.shutdown();
		try {
			sendScheduler.awaitTermination(15, TimeUnit.SECONDS);
			lokiClient.stopLokiClient(15, 15);
		} catch (InterruptedException e) {
		}
		System.err.println(String.format("Total generate log %d", generateLogCount));
		System.err.println(String.format("Total loki stream log %d", totalLokiStream));
	}

}
