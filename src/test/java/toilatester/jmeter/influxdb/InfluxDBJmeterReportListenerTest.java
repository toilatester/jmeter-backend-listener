package toilatester.jmeter.influxdb;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.io.Resources;

import toilatester.jmeter.BaseTest;
import toilatester.jmeter.report.InfluxBackendListener;
import toilatester.jmeter.report.exception.ReportException;

public class InfluxDBJmeterReportListenerTest extends BaseTest {

	@Test
	public void testCanStartJMeterWithDefaultArgumentsConfigListener() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		listener.teardownTest(this.backendListenerContext(this.defaultInfluxDBConfig()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.getRequestedFor(WireMock.urlPathEqualTo("/query")).withQueryParam("q",
						WireMock.equalToIgnoreCase("SHOW DATABASES")));
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/write")));
	}

	@Test
	public void testCanStartJMeterWithDefaultArgumentsConfigListenerAndCreateNewDB() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"another\"]]}]}]}");
		this.influxDBMockServer.stubInfluxPostQueryAPI(200, "CREATE DATABASE \"jmeter\"",
				"{\"results\":[{\"statement_id\":0}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		listener.teardownTest(this.backendListenerContext(this.defaultInfluxDBConfig()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.getRequestedFor(WireMock.urlPathEqualTo("/query")).withQueryParam("q",
						WireMock.equalToIgnoreCase("SHOW DATABASES")));
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/query")).withQueryParam("q",
						WireMock.equalToIgnoreCase("CREATE DATABASE \"jmeter\"")));
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/write")));
	}

	@Test
	public void testCannotStartJMeterWithInvalidConfigListener() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		InfluxBackendListener listener = new InfluxBackendListener();
		Assertions.assertThrows(ReportException.class, () -> {
			listener.setupTest(this.backendListenerContext(new HashMap<String, String>()));
		});
		Assertions.assertEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testSendThreadMetricsSamplerResult() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		listener.run();
		listener.teardownTest(this.backendListenerContext(this.defaultInfluxDBConfig()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.getRequestedFor(WireMock.urlPathEqualTo("/query")).withQueryParam("q",
						WireMock.equalToIgnoreCase("SHOW DATABASES")));
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/write")));
	}

	@Test
	public void testSendPassedSamplerResult() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		listener.teardownTest(this.backendListenerContext(this.defaultInfluxDBConfig()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.getRequestedFor(WireMock.urlPathEqualTo("/query")).withQueryParam("q",
						WireMock.equalToIgnoreCase("SHOW DATABASES")));
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(4),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/write")));
	}
	
	@Test
	public void testSendFailedSamplerResult() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateFailuerSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		listener.teardownTest(this.backendListenerContext(this.defaultInfluxDBConfig()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.getRequestedFor(WireMock.urlPathEqualTo("/query")).withQueryParam("q",
						WireMock.equalToIgnoreCase("SHOW DATABASES")));
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(4),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/write")));
	}

}
