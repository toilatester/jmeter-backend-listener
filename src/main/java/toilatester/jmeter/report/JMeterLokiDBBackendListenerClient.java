package toilatester.jmeter.report;

import java.util.List;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMeterLokiDBBackendListenerClient extends AbstractBackendListenerClient implements Runnable{
	private static final Logger LOGGER = LoggerFactory.getLogger(JMeterLokiDBBackendListenerClient.class);

	@Override
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		LOGGER.info("Process");
		
	}

	@Override
	public void run() {
		
	}
}
