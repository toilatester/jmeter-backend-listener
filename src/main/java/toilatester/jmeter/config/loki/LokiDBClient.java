package toilatester.jmeter.config.loki;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
		this.waitForTerminateThreadPool(this.httpClientThreadPool, clientThreadPoolTimeout, "java http client");
		this.sendLogThreadPool.shutdown();
		this.waitForTerminateThreadPool(this.sendLogThreadPool, sendLogThreadPoolTimeout, "send log");
	}

	private void waitForTerminateThreadPool(ExecutorService threadPool, long timeout, String threadPoolServiceName) {
		try {
			if (!threadPool.awaitTermination(timeout, TimeUnit.SECONDS)) {
				threadPool.shutdownNow();
				if (!threadPool.awaitTermination(timeout, TimeUnit.SECONDS)) {
					LOGGER.info(String.format("Error while wait for all thread pool completed"));
				}
			}

		} catch (InterruptedException e) {
			LOGGER.info(String.format("Error while wait for all %s thread pool completed: %s", threadPoolServiceName,
					e.getCause().toString()));
			sendLogThreadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	public CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
		// Java HttpClient natively supports async API
		// But we have to use its sync API to preserve the ordering of batches
		// This will avoid having issue 'entry out of order' for stream
		if (this.sendLogThreadPool.isShutdown())
			return CompletableFuture
					.completedFuture(new LokiResponse(400, "Re-init LokiDBClient, the current thread pool terminated"));
		return CompletableFuture.supplyAsync(() -> {
			try {
				var request = requestBuilder.copy().POST(HttpRequest.BodyPublishers.ofByteArray(batch)).build();
				var response = client.send(request, HttpResponse.BodyHandlers.ofString());
				return new LokiResponse(response.statusCode(), response.body());

			} catch (Exception innerException) {
				String[] stackTrace = ExceptionUtils.getRootCauseStackTrace(innerException.getCause());
				String errorMessage = String.format("Error: %s \n%s", innerException.getMessage(),
						String.join("\n", stackTrace));
				LOGGER.warn(String.format("Error while sending batch to Loki. %s", errorMessage));
				return new LokiResponse(400, errorMessage);

			}
		}, this.sendLogThreadPool);
	}

	public CompletableFuture<LokiResponse> sendAsyncWithRetry(byte[] batch, int retryNumber) {
		if (this.sendLogThreadPool.isShutdown())
			return CompletableFuture
					.completedFuture(new LokiResponse(400, "Re-init LokiDBClient, the current thread pool terminated"));
		return CompletableFuture.supplyAsync(() -> {
			int enclosingRetryNumber = 0;
			LokiResponse lokiResponse = new LokiResponse(400, "Bad Request");
			while (enclosingRetryNumber < retryNumber) {
				try {
					var request = requestBuilder.copy().POST(HttpRequest.BodyPublishers.ofByteArray(batch)).build();
					var response = client.send(request, HttpResponse.BodyHandlers.ofString());
					int statusCode = response.statusCode();
					if (204 >= statusCode && 200 <= statusCode) {
						return new LokiResponse(response.statusCode(), response.body());
					} else {
						LOGGER.error(String.format("Error while sending batch to Loki %s. Trying to resend %d",
								response.body(), retryNumber));
						lokiResponse.setStatus(response.statusCode());
						lokiResponse.setBody(response.body());
					}
				} catch (Exception innerException) {
					String[] stackTrace = ExceptionUtils.getRootCauseStackTrace(innerException.getCause());
					String errorMessage = String.format("Error: %s \n%s", innerException.getMessage(),
							String.join("\n", stackTrace));
					LOGGER.warn(String.format("Error while sending batch to Loki %s. Trying to resend %d", errorMessage,
							enclosingRetryNumber));
					lokiResponse.setStatus(400);
					lokiResponse.setBody(errorMessage);
				}
				enclosingRetryNumber += 1;
			}
			return lokiResponse;
		}, this.sendLogThreadPool);

	}

}
