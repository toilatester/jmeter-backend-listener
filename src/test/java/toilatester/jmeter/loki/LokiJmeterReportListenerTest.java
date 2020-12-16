package toilatester.jmeter.loki;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.io.Resources;

import toilatester.jmeter.BaseTest;
import toilatester.jmeter.report.LokiBackendListener;

public class LokiJmeterReportListenerTest extends BaseTest {

	@Disabled
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

	@Disabled
	@Test
	public void testCannotStartJMeterWithValidConfigListener() throws Exception {
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
		Thread.sleep(5000);
		this.lokiMockServer.getWireMockServer().verify(1,
				WireMock.postRequestedFor(WireMock.urlEqualTo("/loki/api/v1/push")));
	}
}
