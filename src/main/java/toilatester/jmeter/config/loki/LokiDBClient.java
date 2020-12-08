package toilatester.jmeter.config.loki;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toilatester.jmeter.config.loki.dto.LokiResponse;

public class LokiDBClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(LokiDBClient.class);
	private static final String DEFAULT_CONTENT_TYPE = "application/json";

	private HttpClient client;
	private HttpRequest.Builder requestBuilder;
	private LokiDBConfig config;
	private ExecutorService sendLogThreadPool;
	private ExecutorService httpClientThreadPool;

	public LokiDBClient(LokiDBConfig config, ExecutorService sendLogThreadPool, ExecutorService httpClientThreadPool) {
		this.config = config;
		this.sendLogThreadPool = sendLogThreadPool;
		this.httpClientThreadPool = httpClientThreadPool;
		this.createLokiClient();
	}

	public void createLokiClient() {
		client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(this.config.getLokiConnectiontimeout()))
				.executor(this.httpClientThreadPool).build();

		requestBuilder = HttpRequest.newBuilder().timeout(Duration.ofMillis(this.config.getLokiRequestTimeout()))
				.uri(URI.create(this.config.getLokiUrl())).header("Content-Type", DEFAULT_CONTENT_TYPE);
	}

	public void stopLokiClient() {
		this.httpClientThreadPool.shutdown();
		this.sendLogThreadPool.shutdown();
		try {
			Boolean shutdownNow = !sendLogThreadPool.awaitTermination(3, TimeUnit.SECONDS)
					|| !httpClientThreadPool.awaitTermination(3, TimeUnit.SECONDS);
			if (shutdownNow) {
				httpClientThreadPool.shutdownNow();
				sendLogThreadPool.shutdown();
			}
		} catch (InterruptedException e) {
			httpClientThreadPool.shutdownNow();
		}
	}

	public CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
		// Java HttpClient natively supports async API
		// But we have to use its sync API to preserve the ordering of batches
		return CompletableFuture.supplyAsync(() -> {
			try {
				var request = requestBuilder.copy().POST(HttpRequest.BodyPublishers.ofByteArray(batch)).build();
				var response = client.send(request, HttpResponse.BodyHandlers.ofString());
				return new LokiResponse(response.statusCode(), response.body());
			} catch (Exception e) {
				LOGGER.info(String.format("Error while sending batch to Loki %s", e.getMessage()));
				throw new RuntimeException("Error while sending batch to Loki", e);
			}
		}, this.sendLogThreadPool);
	}

}
