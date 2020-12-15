package toilatester.jmeter.loki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import toilatester.jmeter.config.loki.LokiClientThreadFactory;
import toilatester.jmeter.config.loki.LokiDBClient;
import toilatester.jmeter.config.loki.dto.LokiResponse;

public class LokiClientTest {
	com.github.tomakehurst.wiremock.client.WireMock wireMockClient = new com.github.tomakehurst.wiremock.client.WireMock();
	ExecutorService sendLogThreadPool = Executors.newFixedThreadPool(1,
			new LokiClientThreadFactory("jmeter-send-loki-log"));
	ExecutorService httpClientThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5000 * 10,
			TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(),
			new LokiClientThreadFactory("jmeter-loki-java-http"));

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(
			options().port(3111).httpsPort(3112).jettyHeaderBufferSize(16834).asynchronousResponseEnabled(true)
					.asynchronousResponseThreads(100).jettyAcceptQueueSize(150000));

	@Test
	public void testLokiClientCanConnect() throws IOException, InterruptedException {
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(sendLogThreadPool, httpClientThreadPool);
		client.createLokiClient("http://localhost:3111/loki/api/v1/push", 1000, 1000);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		wireMockRule.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
	}

	@BeforeEach
	public void setUp() {
		wireMockRule.stubFor(
				post(urlPathMatching("/loki/api/v1/push")).withHeader("Content-Type", equalTo("application/json"))
						.willReturn(aResponse().withStatus(200).withBody("Connect")));
		wireMockRule.start();
	}

	@AfterEach
	public void tearDown() {
		wireMockRule.stop();
	}

}
