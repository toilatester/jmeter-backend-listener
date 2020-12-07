package toilatester.jmeter.config.loki;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.loki4j.logback.LokiThreadFactory;

public class LokiDBClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(LokiDBClient.class);

	private HttpClient client;
	private HttpRequest.Builder requestBuilder;
	private LokiDBConfig config;
	private ExecutorService internalHttpThreadPool;
	private static final String DEFAULT_CONTENT_TYPE = "application/json";
	private ExecutorService httpThreadPool;

	public static final class LokiResponse {
		public int status;
		public String body;

		public LokiResponse(int status, String body) {
			this.status = status;
			this.body = body;
		}
	}

	public LokiDBClient(LokiDBConfig config) {
		this.config = config;
		this.createLokiClient();
	}

	public void createLokiClient() {
		httpThreadPool = Executors.newFixedThreadPool(5, new LokiThreadFactory("loki-http-sender"));
		internalHttpThreadPool = new ThreadPoolExecutor(5, Integer.MAX_VALUE, this.config.getLokiBatchTimeout(),
				TimeUnit.MILLISECONDS, // expire unused threads after 15 batch intervals
				new SynchronousQueue<Runnable>(), new LokiThreadFactory("loki-java-http-internal"));

		client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(this.config.getLokiConnectiontimeout()))
				.executor(internalHttpThreadPool).build();

		requestBuilder = HttpRequest.newBuilder().timeout(Duration.ofMillis(this.config.getLokiRequestTimeout()))
				.uri(URI.create(this.config.getLokiUrl())).header("Content-Type", DEFAULT_CONTENT_TYPE);
	}

	public void stopLokiClient() {
		internalHttpThreadPool.shutdown();
	}

	public CompletableFuture<LokiResponse> sendAsync(String batch) {
		// Java HttpClient natively supports async API
		// But we have to use its sync API to preserve the ordering of batches
		if (batch == null || batch.length() == 0)
			LOGGER.info("Empty request body ??????");
		LOGGER.info(batch.toString());
		return CompletableFuture.supplyAsync(() -> {
			try {
				var request = requestBuilder.copy().POST(HttpRequest.BodyPublishers.ofString(batch)).build();

				var response = client.send(request, HttpResponse.BodyHandlers.ofString());

				LOGGER.info(String.format("loki response body %s", response.body()));
				LOGGER.info(String.format("loki response code %d", response.statusCode()));
				return new LokiResponse(response.statusCode(), response.body());
			} catch (Exception e) {
				LOGGER.info(String.format("Exception %s", e.getMessage()));
				throw new RuntimeException("Error while sending batch to Loki", e);
			}
		}, httpThreadPool);
	}

}
