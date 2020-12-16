package toilatester.jmeter.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public class LokiMockServer {
	WireMockServer wireMockServer;
	WireMockConfiguration lokiOptions = options().jettyHeaderBufferSize(16834).asynchronousResponseEnabled(false)
			.asynchronousResponseThreads(100).jettyAcceptQueueSize(150000);

	public LokiMockServer() {
		this.wireMockServer = new WireMockServer(lokiOptions.httpDisabled(false).port(3100).httpsPort(3101));
	}

	public LokiMockServer(boolean enableSsl) {
		if (enableSsl)
			this.wireMockServer = new WireMockServer(lokiOptions.httpsPort(3100));
		else
			this.wireMockServer = new WireMockServer(lokiOptions.httpDisabled(false).port(3100));

	}

	public LokiMockServer(String host, int portNumber, boolean enableSsl) {
		if (enableSsl)
			this.wireMockServer = new WireMockServer(lokiOptions.bindAddress(host).httpsPort(3100));
		else
			this.wireMockServer = new WireMockServer(
					lokiOptions.bindAddress(host).port(portNumber).httpDisabled(false));
	}

	public void startServer() {
		this.wireMockServer.start();
	}

	public void stopServer() {
		this.wireMockServer.stop();
	}

	public void reset() {
		this.wireMockServer.resetAll();
	}

	public String stubResponseData() {
		return this.wireMockServer.baseUrl();
	}

	public void stubLokiPushLogAPI(String stubLog, int statusCode) {
		this.wireMockServer.stubFor(
				post(urlPathMatching("/loki/api/v1/push")).withHeader("Content-Type", equalTo("application/json"))
						.willReturn(aResponse().withStatus(statusCode).withBody(stubLog)));
	}

	public void stubLokiPushThreadMetrics(String stubLog, int statusCode) {
		this.wireMockServer.stubFor(post(urlPathMatching("/loki/api/v1/push"))
				.withHeader("Content-Type", equalTo("application/json"))
				.withRequestBody(
						WireMock.matchingJsonPath("$.streams[0].stream.jmeter_plugin", WireMock.equalTo("loki-log")))
				.willReturn(aResponse().withStatus(statusCode).withBody(stubLog)));
	}

	public WireMockServer getWireMockServer() {
		return this.wireMockServer;
	}

}
