package toilatester.jmeter.influxdb;

import java.util.HashMap;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import toilatester.jmeter.BaseTest;
import toilatester.jmeter.config.influxdb.InfluxDBConfig;
import toilatester.jmeter.report.InfluxBackendListener;
import toilatester.jmeter.report.exception.ReportException;

public class InfluxDBJmeterReportListenerTest extends BaseTest {

	@Test
	public void testCanStartJMeterWithInvalidConfig() {
		InfluxBackendListener listener = new InfluxBackendListener();
		Assertions.assertThrows(ReportException.class, () -> {
			listener.setupTest(new BackendListenerContext(new HashMap<String, String>()));
		});
		Assertions.assertEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testCanStartJMeterWithInvalidProtocolConfig() {
		InfluxBackendListener listener = new InfluxBackendListener();
		Assertions.assertThrows(ReportException.class, () -> {
			listener.setupTest(new BackendListenerContext(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;
				{
					put(InfluxDBConfig.KEY_INFLUX_DB_PROTOCOL, "");
				}
			}));
		});
		Assertions.assertEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testCanStartJMeterWithInvalidHostConfig() {
		InfluxBackendListener listener = new InfluxBackendListener();
		Assertions.assertThrows(ReportException.class, () -> {
			listener.setupTest(new BackendListenerContext(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;

				{
					put(InfluxDBConfig.KEY_INFLUX_DB_PROTOCOL, "http");
					put(InfluxDBConfig.KEY_INFLUX_DB_HOST, "");
				}
			}));
		});
		Assertions.assertEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testCanStartJMeterWithInvalidPortConfig() {
		InfluxBackendListener listener = new InfluxBackendListener();
		Assertions.assertThrows(ReportException.class, () -> {
			listener.setupTest(new BackendListenerContext(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;
				{
					put(InfluxDBConfig.KEY_INFLUX_DB_PROTOCOL, "http");
					put(InfluxDBConfig.KEY_INFLUX_DB_HOST, "localhost");
					put(InfluxDBConfig.KEY_INFLUX_DB_PORT, "");
				}
			}));
		});
		Assertions.assertEquals(0, JMeterContextService.getTestStartTime());
	}

	@Test
	public void testCanStartJMeterWithDefaultArgumentsConfigListener() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
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
	public void testSendPassedSamplerResultWithTransactionName() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		listener.handleSampleResults(
				toilatester.jmeter.utils.JMeterUtils.generateSamplerResultWithSubResultAndTransactionName(101),
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
	public void testSendPassedSamplerResultWithSubResult() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateSamplerResultWithSubResult(101),
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
	public void testSendPassedSamplerResultWithSubResultAndNotIncludSub() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(this.influxDBWithOutSendSubResultConfig()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateSamplerResultWithSubResult(101),
				this.backendListenerContext(this.influxDBWithOutSendSubResultConfig()));
		listener.teardownTest(this.backendListenerContext(this.influxDBWithOutSendSubResultConfig()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(1),
				WireMock.getRequestedFor(WireMock.urlPathEqualTo("/query")).withQueryParam("q",
						WireMock.equalToIgnoreCase("SHOW DATABASES")));
		this.influxDBMockServer.getWireMockServer().verify(WireMock.moreThanOrExactly(4),
				WireMock.postRequestedFor(WireMock.urlPathEqualTo("/write")));
	}

	@Test
	public void testSendPassedSamplerResultWithOutUseRegex() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(this.influxDBWithOutEnableRegexConfig()));
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateSamplerResultWithSubResult(101),
				this.backendListenerContext(this.influxDBWithOutEnableRegexConfig()));
		listener.teardownTest(this.backendListenerContext(this.influxDBWithOutEnableRegexConfig()));
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

	@Test
	public void testCanResendMetricToInfluxDb() throws Exception {
		this.influxDBMockServer.stubInfluxPingAPI(204);
		this.influxDBMockServer.stubInfluxGetQueryAPI(200, "SHOW DATABASES",
				"{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"_internal\"],[\"jmeter\"]]}]}]}");
		this.influxDBMockServer.stubInfluxWriteDataToDatabaseAPI(204, "");
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));

		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateFailuerSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
		this.influxDBMockServer.stopServer();
		listener.handleSampleResults(toilatester.jmeter.utils.JMeterUtils.generateFailuerSamplerResult(101),
				this.backendListenerContext(this.defaultLokiConfig()));
	}

}
