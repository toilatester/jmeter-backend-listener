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
			subResult.setSampleLabel("sub stub label");
			SampleResult result = new SampleResult();
			result.setThreadName("Thread-Group " + i);
			result.setSuccessful(true);
			result.addSubResult(subResult, true);
			result.setSampleLabel("stublabel");
			samplerResults.add(result);
		}
		return samplerResults;
	}

	public static List<SampleResult> generateSamplerResultWithSubResultAndTransactionName(int totalSampler) {
		List<SampleResult> samplerResults = new ArrayList<SampleResult>();
		for (int i = 0; i < totalSampler; i++) {
			SampleResult subResult = new SampleResult();
			subResult.setThreadName("SubResult Thread-Group " + i);
			subResult.setSampleLabel("transaction sub stub");
			subResult.setSuccessful(true);
			SampleResult result1 = new SampleResult();
			result1.setThreadName("Thread-Group " + i);
			result1.setSampleLabel("transactions stub");
			result1.setSuccessful(true);
			result1.addSubResult(subResult, true);
			SampleResult result2 = new SampleResult();
			result2.setThreadName("Thread-Group " + i);
			result2.setSampleLabel("transaction stub");
			result2.setSuccessful(true);
			result2.addSubResult(subResult, true);
			samplerResults.add(result1);
			samplerResults.add(result2);
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
