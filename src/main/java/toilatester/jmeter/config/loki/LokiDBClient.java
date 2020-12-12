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

	public LokiDBClient(ExecutorService sendLogThreadPool, ExecutorService httpClientThreadPool) {
		this.sendLogThreadPool = sendLogThreadPool;
		this.httpClientThreadPool = httpClientThreadPool;
	}

	public void createLokiClient(String lokiUrl, long connectionTimeout, long requestTimeout) {
		client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectionTimeout))
				.executor(this.httpClientThreadPool).build();

		requestBuilder = HttpRequest.newBuilder().timeout(Duration.ofMillis(requestTimeout)).uri(URI.create(lokiUrl))
				.header("Content-Type", DEFAULT_CONTENT_TYPE);
	}

	private void createLokiClient() {
		this.createLokiClient(this.config.getLokiUrl(), this.config.getLokiConnectiontimeout(),
				this.config.getLokiRequestTimeout());
	}

	public void stopLokiClient(long clientThreadPoolTimeout, long sendLogThreadPoolTimeout) {
		this.httpClientThreadPool.shutdown();
		this.waitForClientThreadPoolCompleted(clientThreadPoolTimeout);
		this.sendLogThreadPool.shutdown();
		this.waitForSendLogThreadPoolCompleted(sendLogThreadPoolTimeout);
	}

	private void waitForClientThreadPoolCompleted(long timeout) {
		try {
			if (!this.httpClientThreadPool.awaitTermination(timeout, TimeUnit.SECONDS)) {
				this.httpClientThreadPool.shutdownNow();
				if (!this.httpClientThreadPool.awaitTermination(timeout, TimeUnit.SECONDS)) {
					LOGGER.info(String.format("Error while wait for all thread pool completed"));
				}
			}

		} catch (InterruptedException e) {
			LOGGER.info(String.format("Error while wait for all thread pool completed: %s", e.getMessage()));
			httpClientThreadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private void waitForSendLogThreadPoolCompleted(long timeout) {
		try {
			if (!this.sendLogThreadPool.awaitTermination(timeout, TimeUnit.SECONDS)) {
				this.sendLogThreadPool.shutdownNow();
				if (!this.sendLogThreadPool.awaitTermination(timeout, TimeUnit.SECONDS)) {
					LOGGER.info(String.format("Error while wait for all thread pool completed"));
				}
			}

		} catch (InterruptedException e) {
			LOGGER.info(String.format("Error while wait for all thread pool completed: %s", e.getMessage()));
			sendLogThreadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	public CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
		// Java HttpClient natively supports async API
		// But we have to use its sync API to preserve the ordering of batches
		// This will avoid having issue 'entry out of order' for stream
		System.err.println("Send loki log print in loki client");
		return CompletableFuture.supplyAsync(() -> {
			try {
				System.err.println("Start send loki log print in loki client");
				var request = requestBuilder.copy().POST(HttpRequest.BodyPublishers.ofByteArray(batch)).build();
				var response = client.send(request, HttpResponse.BodyHandlers.ofString());
				System.err.println(response.statusCode());
				System.err.println(response.body());
				return new LokiResponse(response.statusCode(), response.body());
			} catch (Exception e) {
				// TODO return invalid status code to handle in listener
				System.err.println("Error send loki log: "+e.getMessage());
				LOGGER.info(String.format("Error while sending batch to Loki %s", e.getMessage()));
				throw new RuntimeException("Error while sending batch to Loki", e);
			}
		}, this.sendLogThreadPool);
	}

}
