package toilatester.jmeter.config.loki.dto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import toilatester.jmeter.config.loki.LokiClientThreadFactory;

public class Main {
	private static int generateLogCount = 0;
	private static int sendLogCount = 0;
	private static String LOCAL_URL = "http://localhost:8080/loki/api/v1/push";
	private static String LOKI_URL = "http://192.168.10.69:30842/loki/api/v1/push";
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
			lokiStream.setStream(labels);
			int totalRandomLog = new Random().nextInt(5000);
			System.err.println(String.format("Total Generate Log %d", totalRandomLog));
			List<List<String>> listLog = new ArrayList<>();
			for (int i = 0; i < totalRandomLog; i++) {
				listLog.add(new LokiLog("Sample log " + i + " " + Long.toString(System.currentTimeMillis() * 1000000))
						.getLogObject());
			}
			lokiStream.setValues(listLog);
			lokiStreams.setStreams(Arrays.asList(lokiStream));
			this.queue.add(lokiStreams);
		}

		public void addSeperateLog() {
			Map<String, String> labels = new HashMap<>();
			labels.put("jmeter_plugin", "toilatester");
			labels.put("external_label", "minhhoang");
			int totalRandomLog = new Random().nextInt(5000);
			generateLogCount += totalRandomLog;
			System.err.println(String.format("Total Generate Log %d", totalRandomLog));
			for (int i = 0; i < totalRandomLog; i++) {
				LokiStreams lokiStreams = new LokiStreams();
				LokiStream lokiStream = new LokiStream();
				lokiStream.setStream(labels);
				List<List<String>> listLog = new ArrayList<>();
				listLog.add(new LokiLog("Sample log " + i + " " + Long.toString(System.currentTimeMillis() * 1000000))
						.getLogObject());
				lokiStream.setValues(listLog);
				lokiStreams.setStreams(Arrays.asList(lokiStream));
				this.queue.add(lokiStreams);
			}

		}

	}

	private static class SendLokiLog {
		private Queue<LokiStreams> queue;
		private HttpClient client;
		private HttpRequest.Builder requestBuilder;
		private ExecutorService httpPool;
		private static final String DEFAULT_CONTENT_TYPE = "application/json";

		public SendLokiLog(Queue<LokiStreams> queue) {
			this.queue = queue;
			this.httpPool = Executors.newFixedThreadPool(50);
			this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000)).executor(this.httpPool)
					.build();

			this.requestBuilder = HttpRequest.newBuilder().timeout(Duration.ofMillis(5000)).uri(URI.create(LOCAL_URL))
					.header("Content-Type", DEFAULT_CONTENT_TYPE);
		}

		public void shutDownHttpPool() {
			if (this.queue.size() > 0)
				this.sendLog();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Just for debug purpose
			}
			this.httpPool.shutdown();
		}

		public void sendLog() {
			ObjectMapper mapper = new ObjectMapper();
			System.err.println(String.format("Total log to send %d", queue.size()));
			int queueLength = this.queue.size();
			while (queueLength > 0) {
				
				try {
					String requestJSON = mapper.writeValueAsString(this.queue.poll());
					sendLogCount += 1;
					var request = this.requestBuilder.copy()
							.POST(HttpRequest.BodyPublishers.ofByteArray(requestJSON.getBytes())).build();
					var response = this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
					response.thenAccept(res -> {
						System.out.println(String.format("Status code: %d", res.statusCode()));
					});

				} catch (Exception e) {
					e.printStackTrace();
				}
				queueLength = this.queue.size();
			}
		}

	}

	/**
	 * Just for debug and verify solution :(
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.err.println("===============================");
		System.err.println("===============================");
		System.err.println("===============================");
		Instant start = Instant.now();
		Instant stop = Instant.now();
		Duration limit = Duration.ofSeconds(30);
		Duration duration = Duration.between(start, stop);
		queue = new LinkedBlockingDeque<LokiStreams>();
		AddLokiLog addLog = new AddLokiLog(queue);
		SendLokiLog sendLog = new SendLokiLog(queue);
		addSchedulerSession = addScheduler.scheduleAtFixedRate(() -> addLog.addSeperateLog(), 500, 1000,
				TimeUnit.MILLISECONDS);
		sendSchedulerSession = sendScheduler.scheduleAtFixedRate(() -> sendLog.sendLog(), 500, 2000,
				TimeUnit.MILLISECONDS);
		while ((duration.compareTo(limit) <= 0)) {
			duration = Duration.between(start, Instant.now());
		}
		addSchedulerSession.cancel(true);
		addScheduler.shutdown();
		System.err.println(String.format("Current queue size after shutdown add: %d", queue.size()));
		while (queue.size() > 0) {
			;
			;
		}
		System.err.println("Completed send all log!!!!!!!!!!!!!!!!!!!!!!!!");
		sendLog.shutDownHttpPool();
		sendSchedulerSession.cancel(true);
		sendScheduler.shutdown();
		System.err.println(String.format("Total generate log %d", generateLogCount));
		System.err.println(String.format("Total send log %d", sendLogCount));
	}

}
