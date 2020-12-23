package toilatester.jmeter.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public class InfluxDBMockServer {
	WireMockServer wireMockServer;
	WireMockConfiguration lokiOptions = options().jettyHeaderBufferSize(16834).asynchronousResponseEnabled(true)
			.asynchronousResponseThreads(100).jettyAcceptQueueSize(150000);

	public InfluxDBMockServer() {
		this.wireMockServer = new WireMockServer(lokiOptions.httpDisabled(false).port(8086).httpsPort(8087));
	}

	public InfluxDBMockServer(boolean enableSsl) {
		if (enableSsl)
			this.wireMockServer = new WireMockServer(lokiOptions.httpsPort(8086));
		else
			this.wireMockServer = new WireMockServer(lokiOptions.httpDisabled(false).port(8087));

	}

	public InfluxDBMockServer(String host, int portNumber, boolean enableSsl) {
		if (enableSsl)
			this.wireMockServer = new WireMockServer(lokiOptions.bindAddress(host).httpsPort(8086));
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

	public void stubInfluxWriteDataToDatabaseAPI(int statusCode, String responseData) {
		this.wireMockServer.stubFor(post(WireMock.urlPathEqualTo("/write"))
				.withQueryParam("u", WireMock.equalTo("default")).withQueryParam("p", WireMock.equalTo("default"))
				.withQueryParam("db", WireMock.equalTo("jmeter")).withQueryParam("rp", WireMock.equalTo("autogen"))
				.withQueryParam("precision", WireMock.equalTo("n"))
				.withQueryParam("consistency", WireMock.equalTo("one"))
				.willReturn(aResponse().withStatus(statusCode).withBody(responseData)));
	}

	public void stubInfluxPingAPI(int statusCode) {
		this.wireMockServer.stubFor(get(WireMock.urlPathEqualTo("/ping")).willReturn(aResponse().withStatus(204)));
	}

	public void stubInfluxGetQueryAPI(int statusCode, String queryData, String responseData) {
		this.wireMockServer.stubFor(get(WireMock.urlPathEqualTo("/query"))
				.withQueryParam("u", WireMock.equalTo("default")).withQueryParam("p", WireMock.equalTo("default"))
				.withQueryParam("q", WireMock.equalToIgnoreCase(queryData))
				.willReturn(aResponse().withStatus(statusCode).withBody(responseData)));
	}

	public void stubInfluxPostQueryAPI(int statusCode, String queryData, String responseData) {
		this.wireMockServer.stubFor(post(WireMock.urlPathEqualTo("/query"))
				.withQueryParam("u", WireMock.equalTo("default")).withQueryParam("p", WireMock.equalTo("default"))
				.withQueryParam("q", WireMock.equalToIgnoreCase(queryData))
				.willReturn(aResponse().withStatus(statusCode).withBody(responseData)));
	}

	public WireMockServer getWireMockServer() {
		return this.wireMockServer;
	}

}
