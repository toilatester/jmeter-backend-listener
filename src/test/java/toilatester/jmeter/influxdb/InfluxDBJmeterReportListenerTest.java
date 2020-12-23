package toilatester.jmeter.influxdb;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.io.Resources;

import toilatester.jmeter.BaseTest;
import toilatester.jmeter.report.InfluxBackendListener;

public class InfluxDBJmeterReportListenerTest extends BaseTest {

	@Test
	public void testCanStartJMeterWithDefaultArgumentsConfigListener() throws Exception {
		URL url = Resources.getResource("jmeter.properties");
		Path path = Paths.get(url.toURI());
		JMeterUtils.getProperties(path.toString());
		JMeterContextService.startTest();
		InfluxBackendListener listener = new InfluxBackendListener();
		listener.setupTest(new BackendListenerContext(listener.getDefaultParameters()));
		Assertions.assertNotEquals(0, JMeterContextService.getTestStartTime());
		listener.teardownTest(this.backendListenerContext(this.defaultInfluxDBConfig()));
	}

}
