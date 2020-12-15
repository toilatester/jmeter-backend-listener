package toilatester.jmeter.utils;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

public class LokiMockServer {
	WireMockServer wireMockServer;

	public LokiMockServer() {
		this.wireMockServer = new WireMockServer(wireMockConfig().httpDisabled(false).port(3100));
	}

	public LokiMockServer(boolean enableSsl) {
		if (enableSsl)
			this.wireMockServer = new WireMockServer(wireMockConfig().httpsPort(3100));
		else
			this.wireMockServer = new WireMockServer(wireMockConfig().httpDisabled(false).port(3100));

	}

	public LokiMockServer(String host, int portNumber, boolean enableSsl) {
		if (enableSsl)
			this.wireMockServer = new WireMockServer(wireMockConfig().bindAddress(host).httpsPort(3100));
		else
			this.wireMockServer = new WireMockServer(wireMockConfig().bindAddress(host).port(portNumber).httpDisabled(false));
	}

	public void startServer() {
		this.wireMockServer.start();
	}
	
	public void stopServer() {
		this.wireMockServer.stop();
	}
	
	public String stubResponseData() {
		return this.wireMockServer.baseUrl();
	}
	
	public void stubLokiAPI() {
		this.wireMockServer.stubFor(get(urlPathMatching("/loki/api/v1/*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("\"testing-library\": \"WireMock\"")));
	}

}
