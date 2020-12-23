package toilatester.jmeter.loki;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import toilatester.jmeter.BaseTest;
import toilatester.jmeter.config.loki.LokiDBClient;
import toilatester.jmeter.config.loki.LokiDBConfig;
import toilatester.jmeter.config.loki.dto.LokiResponse;

public class LokiClientTest extends BaseTest {

	@Test
	public void testLokiClientCanConnectWithStatus200() throws IOException, InterruptedException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 200);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 3000, 3000);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(1,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		client.stopLokiClient(1, 1);
	}

	@Test
	public void testLokiClientCanConnectWithStatus204() throws IOException, InterruptedException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 3000, 3000);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(1,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		client.stopLokiClient(1, 1);
	}


	@Test
	public void testLokiClientCanConnectWithStatus201() throws IOException, InterruptedException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 201);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 3000, 3000);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(1,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		client.stopLokiClient(1, 1);
	}

	
	@Test
	public void testLokiClientSendLogRetrySuccessWithStatus200() {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 200);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 3000, 3000);
		client.sendAsyncWithRetry("Hello".getBytes(), 3).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(1,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		client.stopLokiClient(1, 1);
	}

	@Test
	public void testLokiClientSendLogRetrySuccessWithStatus204() {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 3000, 3000);
		client.sendAsyncWithRetry("Hello".getBytes(), 3).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(1,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		client.stopLokiClient(1, 1);
	}

	@Test
	public void testLokiClientSendLogWithMaxRetry() {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 400);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 3000, 3000);
		client.sendAsyncWithRetry("Hello".getBytes(), 3).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(3,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		client.stopLokiClient(1, 1);
	}

	@Test
	public void testLokiClientCannotConnectAndReturn400Code()
			throws IOException, InterruptedException, ExecutionException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 400);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 1, 1);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(0,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals(400, future.get().getStatus());
		client.stopLokiClient(1, 1);
	}

	@Test
	public void testLokiClientCannotConnectAndReturn400CodeWithRetryCase400()
			throws IOException, InterruptedException, ExecutionException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 400);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 1, 1);
		client.sendAsyncWithRetry("Hello".getBytes(), 3).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(0,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals(400, future.get().getStatus());
		client.stopLokiClient(1, 1);
	}
	
	@Test
	public void testLokiClientCannotConnectAndReturn400CodeWithRetryCase302()
			throws IOException, InterruptedException, ExecutionException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 302);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 1, 1);
		client.sendAsyncWithRetry("Hello".getBytes(), 3).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(0,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals(400, future.get().getStatus());
		client.stopLokiClient(1, 1);
	}
	
	@Test
	public void testLokiClientCannotConnectAndReturn400CodeWithRetryCase101()
			throws IOException, InterruptedException, ExecutionException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 101);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 1, 1);
		client.sendAsyncWithRetry("Hello".getBytes(), 3).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(0,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals(400, future.get().getStatus());
		client.stopLokiClient(1, 1);
	}

	@Test
	public void testLokiClientCantSendWhenStop() throws InterruptedException, ExecutionException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 400);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 1, 1);
		client.stopLokiClient(1, 1);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(0,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals(400, future.get().getStatus());
		Assertions.assertEquals("Re-init LokiDBClient, the current thread pool terminated", future.get().getBody());
		client.stopLokiClient(1, 1);
	}

	@Test
	public void testLokiClientRetryToTerminateThreadPool() throws InterruptedException, ExecutionException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 200);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 3000, 3000);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		client.stopLokiClient(0, 0);
		this.lokiMockServer.getWireMockServer().verify(1,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals(200, future.get().getStatus());
		client.stopLokiClient(0, 0);
	}

	@Test
	public void testLokiClientCantSendWithRetryWhenStop() throws InterruptedException, ExecutionException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 400);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 1, 1);
		client.stopLokiClient(1, 1);
		client.sendAsyncWithRetry("Hello".getBytes(), 3).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(0,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals(400, future.get().getStatus());
		Assertions.assertEquals("Re-init LokiDBClient, the current thread pool terminated", future.get().getBody());
		client.stopLokiClient(1, 1);
	}

	@Test
	public void testInitLokiClientWithJMeterConfig() throws InterruptedException, ExecutionException {
		LokiDBConfig lokiDbConfig = this.lokiDbConfig(this.defaultLokiConfig());
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 200);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(lokiDbConfig, this.sendLogThreadPool, this.httpClientThreadPool);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		this.lokiMockServer.getWireMockServer().verify(1,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		client.stopLokiClient(1, 1);
	}
}
