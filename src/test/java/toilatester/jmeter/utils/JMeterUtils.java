package toilatester.jmeter.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;

public class JMeterUtils {

	public static List<SampleResult> generateSamplerResult(int totalSampler) {
		List<SampleResult> samplerResults = new ArrayList<SampleResult>();
		for (int i = 0; i < totalSampler; i++) {
			SampleResult result = new SampleResult();
			result.setThreadName("Thread-Group " + i);
			result.setSuccessful(true);
			samplerResults.add(result);
		}
		return samplerResults;
	}

	public static List<SampleResult> generateSamplerResultWithSubResult(int totalSampler) {
		List<SampleResult> samplerResults = new ArrayList<SampleResult>();
		for (int i = 0; i < totalSampler; i++) {
			SampleResult subResult = new SampleResult();
			subResult.setThreadName("SubResult Thread-Group " + i);
			subResult.setSuccessful(true);
			SampleResult result = new SampleResult();
			result.setThreadName("Thread-Group " + i);
			result.setSuccessful(true);
			result.addSubResult(subResult, true);
			samplerResults.add(result);
		}
		return samplerResults;
	}

	public static List<SampleResult> generateFailuerSamplerResult(int totalSampler) {
		List<SampleResult> samplerResults = new ArrayList<SampleResult>();
		for (int i = 0; i < totalSampler; i++) {
			SampleResult result = new SampleResult();
			AssertionResult assertFailuer = new AssertionResult("Stub Failuer Sampler");
			assertFailuer.setFailure(true);
			assertFailuer.setFailureMessage("Failuer Message Stub");
			result.setThreadName("Thread-Group " + i);
			result.setSuccessful(false);
			result.addAssertionResult(assertFailuer);
			samplerResults.add(result);
		}
		return samplerResults;
	}
}
