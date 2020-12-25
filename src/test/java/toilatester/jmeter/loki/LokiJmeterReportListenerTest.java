package toilatester.jmeter.loki;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import toilatester.jmeter.BaseTest;
import toilatester.jmeter.config.loki.dto.LokiStreams;
import toilatester.jmeter.report.LokiBackendListener;
import toilatester.jmeter.report.exception.ReportException;

public class LokiJmeterReportListenerTest extends BaseTest {

	@Test
	public void testCanStartJMeterWithValidConfigListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testCanStartJMeterWithDefaultArgumentsConfigListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testCannotStartJMeterWithInvalidConfigListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		Assertions.assertThrows(ReportException.class, () -> {
			listener.setupTest(this.backendListenerContext(new HashMap<String, String>()));
		});
		Assertions.assertEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testCanSendLogJMeterWithValidConfigListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.run();
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);

		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		String jsonBodyRequest = request.stream().filter(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics");
			return lokiLabel != null && lokiLabel.contains("thread-metrics");
		}).findFirst().get().getBodyAsString();
		LokiStreams streams = this.deserializeJSONToObject(jsonBodyRequest, LokiStreams.class);

		this.lokiMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals("loki-log", streams.getStreams().get(0).getStream().get("jmeter_plugin"));
		Assertions.assertEquals("thread-metrics", streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics"));
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testCanSendLogJMeterWithValidConfigLogAllResponseDataListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.lokiConfigWithLogAllResponseData()));
		listener.run();
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		String jsonBodyRequest = request.stream().filter(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics");
			return lokiLabel != null && lokiLabel.contains("thread-metrics");
		}).findFirst().get().getBodyAsString();
		LokiStreams streams = this.deserializeJSONToObject(jsonBodyRequest, LokiStreams.class);
		this.lokiMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals("loki-log", streams.getStreams().get(0).getStream().get("jmeter_plugin"));
		Assertions.assertEquals("thread-metrics", streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics"));
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testCanSendLogJMeterWithExternalLabelsConfigListener() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.lokiConfigWithExternalLabel()));
		listener.run();
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		String jsonBodyRequest = request.stream().filter(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_plugin_metrics");
			return lokiLabel != null && lokiLabel.contains("thread-metrics");
		}).findFirst().get().getBodyAsString();
		LokiStreams streams = this.deserializeJSONToObject(jsonBodyRequest, LokiStreams.class);
		this.lokiMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals("loki-log", streams.getStreams().get(0).getStream().get("jmeter_plugin"));
		Assertions.assertEquals("external-labels", streams.getStreams().get(0).getStream().get("toilatesterwithspace"));
		Assertions.assertEquals("external-labels", streams.getStreams().get(0).getStream().get("toilatester"));
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testSendLokiLogInHandlerSampler() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_data");
			return lokiLabel != null && lokiLabel.contains("response-data");
		});
		Assertions.assertEquals(true, recieveRequest);
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testSendLokiLogInHandlerSamplerWithSubResult() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateSamplerResultWithSubResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_data");
			return lokiLabel != null && lokiLabel.contains("response-data");
		});
		Assertions.assertEquals(true, recieveRequest);
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testSendLokiLogInTearDownWithForceSendResult() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.lokiConfigWithTurnOffThreadMetrics()));
		for (int samplerCount = 0; samplerCount < 3001; samplerCount++) {
			listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateSamplerResult(10),
					this.backendListenerContext(this.lokiConfigWithTurnOffThreadMetrics()));
		}
		Thread.sleep(3100);
		listener.teardownTest(this.backendListenerContext(this.lokiConfigWithTurnOffThreadMetrics()));
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_data");
			return lokiLabel != null && lokiLabel.contains("response-data");
		});
		this.lokiMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(300),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/loki/api/v1/push")));
		Assertions.assertEquals(true, recieveRequest);
	}

	@Test
	public void testSendLokiLogInHandlerSamplerWithFailuerSampler() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateFailuerSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_data");
			return lokiLabel != null && lokiLabel.contains("response-data");
		});
		Assertions.assertEquals(true, recieveRequest);
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testSendLokiLogInHandlerSamplerWithFailuerSamplerAndLogAllResponseData() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.lokiConfigWithLogAllResponseData()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateFailuerSamplerResult(101),
				this.backendListenerContext(this.lokiConfigWithLogAllResponseData()));
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_data");
			return lokiLabel != null && lokiLabel.contains("response-data");
		});
		Assertions.assertEquals(true, recieveRequest);
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	@Test
	public void testSendLokiLogInTeardown() throws Exception {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 204);
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
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
		LokiBackendListener listener = new LokiBackendListener();
		listener.setupTest(this.backendListenerContext(this.defaultLokiConfig()));
		for (Method m : listener.getClass().getDeclaredMethods()) {
			if (m.getName() == "sendErrorLog") {
				m.setAccessible(true);
				for (int count = 0; count < 10; count++)
					m.invoke(listener, "Stub error logs");
			}
		}
		waitToReceiveData.apply(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")), 3);
		List<LoggedRequest> request = this.lokiMockServer.getWireMockServer()
				.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build())
				.getRequests();
		boolean recieveRequest = request.parallelStream().anyMatch(logRequest -> {
			LokiStreams streams = this.deserializeJSONToObject(logRequest.getBodyAsString(), LokiStreams.class);
			String lokiLabel = streams.getStreams().get(0).getStream().get("jmeter_plugin");
			return lokiLabel != null && lokiLabel.contains("errors");
		});
		Assertions.assertEquals(true, recieveRequest);
		listener.teardownTest(this.backendListenerContext(this.defaultLokiConfig()));
	}

	protected BiFunction<RequestPatternBuilder, Integer, List<LoggedRequest>> waitToReceiveData = (pattern, retry) -> {
		while (retry > 0) {
			FindRequestsResult result = this.lokiMockServer.getWireMockServer()
					.findRequestsMatching(WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")).build());
			if (result.getRequests().size() > 3) {
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
}
