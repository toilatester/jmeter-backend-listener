package toilatester.jmeter.config.loki.dto;

import java.io.IOException;
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
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import toilatester.jmeter.config.loki.LokiClientThreadFactory;

public class Main {
	public static int count = 0;
	private static BlockingQueue<LokiStreams> queue;
	private static ScheduledExecutorService addScheduler = Executors.newScheduledThreadPool(1,
			new LokiClientThreadFactory("loki-scheduler-add"));;
	private static ScheduledFuture<?> addSchedulerSession;
	private static ScheduledExecutorService sendScheduler = Executors.newScheduledThreadPool(1,
			new LokiClientThreadFactory("loki-scheduler-send"));;
	private static ScheduledFuture<?> sendSchedulerSession;

	private static class AddLokiLog implements Runnable {
		private BlockingQueue<LokiStreams> queue;

		public AddLokiLog(BlockingQueue<LokiStreams> queue) {
			this.queue = queue;
		}

		@Override
		public void run() {
			LokiStreams lokiStreams = new LokiStreams();
			LokiStream lokiStream = new LokiStream();
			lokiStream.setStream(new HashMap<String, String>() {
				{
					put("jmeter_plugin", "toilatester");
					put("external_label", "minhhoang");
				}
			});
			int totalRandomLog = new Random().nextInt(500);
			System.err.println(String.format("Total Generate Log %d", totalRandomLog));
			List<List<String>> listLog = new ArrayList<>();
			for (int i = 0; i < totalRandomLog; i++) {
				listLog.add(new LokiLog("Sample log " + i + " " + Long.toString(System.currentTimeMillis() * 1000000))
						.getLogObject());
//				lokiStream.setValues(Arrays.asList(new LokiLog("Sample log 1").getLogObject(),
//						new LokiLog("Sample log 2").getLogObject(), new LokiLog("Sample log 3").getLogObject()));
//				lokiStreams.setStreams(Arrays.asList(lokiStream));
			}
			lokiStream.setValues(listLog);
			lokiStreams.setStreams(Arrays.asList(lokiStream));
			this.queue.add(lokiStreams);
		}

	}

	private static class SendLokiLog implements Runnable {
		private BlockingQueue<LokiStreams> queue;
		private HttpClient client;
		private HttpRequest.Builder requestBuilder;
		private static final String DEFAULT_CONTENT_TYPE = "application/json";

		public SendLokiLog(final BlockingQueue<LokiStreams> queue) {
			this.queue = queue;
			this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000))
					.executor(Executors.newFixedThreadPool(50)).build();

			this.requestBuilder = HttpRequest.newBuilder().timeout(Duration.ofMillis(5000))
					.uri(URI.create("http://192.168.10.69:30842/loki/api/v1/push"))
					.header("Content-Type", DEFAULT_CONTENT_TYPE);
		}

		@Override
		public void run() {
			ObjectMapper mapper = new ObjectMapper();
			System.err.println(String.format("Total log to send %d", queue.size()));
			while (!this.queue.isEmpty()) {
				try {
					String requestJSON = mapper.writeValueAsString(this.queue.poll());
					var request = this.requestBuilder.copy()
							.POST(HttpRequest.BodyPublishers.ofByteArray(requestJSON.getBytes())).build();
					var response = this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
					response.thenAccept(r -> {
						System.out.println(String.format("Body : %s", r.body()));
						System.out.println(String.format("Status code: %d", r.statusCode()));
					});
				} catch (IOException e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}

			}
			System.out.println(String.format("Completed send all log  %d", queue.size()));
		}

	}

	public static void main(String[] args) throws JsonProcessingException {
		Instant start = Instant.now();
		Instant stop = Instant.now();
		Duration limit = Duration.ofSeconds(3);
		Duration duration = Duration.between(start, stop);
		queue = new LinkedBlockingDeque<LokiStreams>();
		Runnable addLog = new AddLokiLog(queue);
		Runnable sendLog = new SendLokiLog(queue);
		addSchedulerSession = addScheduler.scheduleAtFixedRate(addLog, 500, 1000, TimeUnit.MILLISECONDS);
		sendSchedulerSession = sendScheduler.scheduleAtFixedRate(sendLog, 500, 500, TimeUnit.MILLISECONDS);
		while ((duration.compareTo(limit) <= 0)) {
			duration = Duration.between(start, Instant.now());
		}
		addSchedulerSession.cancel(true);
		addScheduler.shutdown();
		while (!addScheduler.isTerminated()) {
			addScheduler.shutdownNow();

		}
		System.err.println(String.format("Current queue size after shutdown add: %d", queue.size()));
		while (queue.size() != 0) {
			;
		}
		System.err.println(String.format("Current queue size after completed send: %d", queue.size()));
		if (queue.isEmpty()) {
			sendSchedulerSession.cancel(true);
			sendScheduler.shutdown();
		}
		while (!sendScheduler.isTerminated()) {
			sendScheduler.shutdownNow();

		}
//		LokiStreams lokiStreams = new LokiStreams();
//		LokiStream lokiStream = new LokiStream();
//		for (int i = 0; i < Integer.MAX_VALUE; i++) {
//			count++;
//			lokiStream.setValues(Arrays.asList(new LokiLog("Sample log 1").getLogObject(),
//					new LokiLog("Sample log 2").getLogObject(), new LokiLog("Sample log 3").getLogObject()));
//			lokiStream.setStream(new HashMap<String, String>() {
//				{
//					put("jmeter_plugin", "toilatester");
//					put("external_label", "minhhoang");
//					put("stream", Integer.toString(count));
//				}
//			});
//			lokiStreams.setStreams(Arrays.asList(lokiStream));
//			queue.add(lokiStreams);
//		}
//		while (!queue.isEmpty()) {
//			System.out.println(queue.size());
//			ObjectMapper mapper = new ObjectMapper();
//			String requestJSON = mapper.writeValueAsString(queue.poll());
//			System.out.println(requestJSON);
//		}

	}

}
