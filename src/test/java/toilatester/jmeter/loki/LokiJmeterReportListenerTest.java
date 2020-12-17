package toilatester.jmeter.loki;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.io.Resources;

import toilatester.jmeter.BaseTest;
import toilatester.jmeter.config.loki.dto.LokiStreams;
import toilatester.jmeter.report.LokiBackendListener;

public class LokiJmeterReportListenerTest extends BaseTest {

	@Test
	public void testCanStartJMeterWithValidConfigListener() throws Exception {
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testCanStartJMeterWithDefaultArgumentsConfigListener() throws Exception {
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testCannotStartJMeterWithInvalidConfigListener() throws Exception {
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			listener.setupTest(this.backendListenerContext(new HashMap<String, String>()));
		});
		Assertions.assertEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testCanSendLogJMeterWithValidConfigListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.run();
		LoggedRequest request = waitToReceiveData
				.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3).get(0);
		LokiStreams streams = this.deserializeJSONToObject(request.getBodyAsString(), LokiStreams.class);
		this.lokiMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals("loki-log", streams.getStreams().get(0).getStream().get("jmeter_plugin"));
		Assertions.assertEquals("thread-metrics", streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics"));
	}
	
	@Test
	public void testCanSendLogJMeterWithValidConfigLogAllResponseDataListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.lokiConfigWithLogAllResponseData()));
		listener.run();
		LoggedRequest request = waitToReceiveData
				.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3).get(0);
		LokiStreams streams = this.deserializeJSONToObject(request.getBodyAsString(), LokiStreams.class);
		this.lokiMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals("loki-log", streams.getStreams().get(0).getStream().get("jmeter_plugin"));
		Assertions.assertEquals("thread-metrics", streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics"));
	}


	@Test
	public void testCanSendLogJMeterWithExternalLabelsConfigListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.lokiConfigWithExternalLabel()));
		listener.run();
		LoggedRequest request = waitToReceiveData
				.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3).get(0);
		LokiStreams streams = this.deserializeJSONToObject(request.getBodyAsString(), LokiStreams.class);
		this.lokiMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals("loki-log", streams.getStreams().get(0).getStream().get("jmeter_plugin"));
		Assertions.assertEquals("thread-metrics", streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics"));
		Assertions.assertEquals("external-labels", streams.getStreams().get(0).getStream().get("toilatesterwithspace"));
		Assertions.assertEquals("external-labels", streams.getStreams().get(0).getStream().get("toilatester"));
	}


	@Test
	public void testSendLokiLogInHandlerSampler() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.handleSampleResults(this.generateSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		Thread.sleep(2000);
		List<LoggedRequest> request = waitToReceiveData
				.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_data");
			return lokiLabel != null && lokiLabel.contains("response-data");
		});
		Assertions.assertEquals(true, recieveRequest);
	}
	
	@Test
	public void testSendLokiLogInHandlerSamplerWithFailuerSampler() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.handleSampleResults(this.generateFailuerSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		Thread.sleep(2000);
		List<LoggedRequest> request = waitToReceiveData
				.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_data");
			return lokiLabel != null && lokiLabel.contains("response-data");
		});
		Assertions.assertEquals(true, recieveRequest);
	}
	
	@Test
	public void testSendLokiLogInHandlerSamplerWithFailuerSamplerAndLogAllResponseData() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.lokiConfigWithLogAllResponseData()));
		listener.handleSampleResults(this.generateFailuerSamplerResult(101),
				this.backendListenerContext(this.lokiConfigWithLogAllResponseData()));
		Thread.sleep(2000);
		List<LoggedRequest> request = waitToReceiveData
				.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_data");
			return lokiLabel != null && lokiLabel.contains("response-data");
		});
		Assertions.assertEquals(true, recieveRequest);
	}

	@Test
	public void testSendLokiLogInTeardown() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.handleSampleResults(this.generateSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
		Thread.sleep(2000);
		List<LoggedRequest> request = waitToReceiveData
				.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics");
			return lokiLabel != null && lokiLabel.contains("sampler-metrics");
		});
		Assertions.assertEquals(true, recieveRequest);
	}

	@Test
	public void testSendLokiErrorLog() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		for (Method m : listener.getClass().getDeclaredMethods()) {
			if (m.getName() == "sendErrorLog") {
				m.setAccessible(true);
				for (int count = 0; count < 10; count++)
					m.invoke(listener, "Stub error logs");
			}
		}
		List<LoggedRequest> request = waitToReceiveData
				.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_plugin");
			return lokiLabel != null && lokiLabel.contains("errors");
		});
		Assertions.assertEquals(true, recieveRequest);
	}

	private BiFunction<RequestPatternBuilder, Integer, List<LoggedRequest>> waitToReceiveData = (pattern, retry) -> {
		while (retry > 0) {
			FindRequestsResult result = this.lokiMockServer.getWireMockServer()
					.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build());
			if (result.getRequests().size() > 0) {
				return result.getRequests();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			retry -= 1;
		}
		return new ArrayList<>();
	};

	private List<SampleResult> generateSamplerResult(int totalSampler) {
		List<SampleResult> samplerResults = new ArrayList<SampleResult>();
		for (int i = 0; i < totalSampler; i++) {
			SampleResult result = new SampleResult();
			result.setThreadName("Thread-Group " + i);
			result.setSuccessful(true);
			samplerResults.add(result);
		}
		return samplerResults;
	}

	private List<SampleResult> generateFailuerSamplerResult(int totalSampler) {
		List<SampleResult> samplerResults = new ArrayList<SampleResult>();
		for (int i = 0; i < totalSampler; i++) {
			SampleResult result = new SampleResult();
			result.setThreadName("Thread-Group " + i);
			result.setSuccessful(false);
			result.addAssertionResult(new AssertionResult("Stub Failuer Sampler"));
			samplerResults.add(result);
		}
		return samplerResults;
	}

}
