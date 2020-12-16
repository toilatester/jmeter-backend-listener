package toilatester.jmeter.loki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import toilatester.jmeter.BaseTest;
import toilatester.jmeter.config.loki.LogLevel;
import toilatester.jmeter.config.loki.LokiDBClient;
import toilatester.jmeter.config.loki.dto.LokiLog;
import toilatester.jmeter.config.loki.dto.LokiResponse;
import toilatester.jmeter.config.loki.dto.LokiStream;
import toilatester.jmeter.config.loki.dto.LokiStreams;

public class LokiDtoParsingTest extends BaseTest {

	private LokiLog infoLog = new LokiLog(LogLevel.INFO.value(), "This is test info log");
	private LokiLog warnLog = new LokiLog(LogLevel.WARN.value(), "This is test warn log");
	private LokiLog errorLog = new LokiLog(LogLevel.ERROR.value(), "This is test error log");

	@Test
	public void testParsingTextWithOutSpcialChar() throws JsonProcessingException {
		Map<String, String> labels = new HashMap<>();
		String expectedLog = String.format(
				"{\"streams\":[{\"stream\":{\"test_label\":\"labels-value\"},\"values\":[[\"%d\",\"[INFO] This is test info log\"],[\"%d\",\"[WARN] This is test warn log\"],[\"%d\",\"[ERROR] This is test error log\"]]}]}",
				infoLog.getUnixEpochNano(), warnLog.getUnixEpochNano(), errorLog.getUnixEpochNano());
		labels.put("test_label", "labels-value");
		LokiStreams lokiStreams = new LokiStreams();
		List<LokiStream> lokiStreamList = new ArrayList<>();
		List<List<String>> listLog = new ArrayList<>();
		LokiStream lokiStream = new LokiStream();
		lokiStream.setStream(labels);
		listLog.add(infoLog.getLogObject());
		listLog.add(warnLog.getLogObject());
		listLog.add(errorLog.getLogObject());
		lokiStream.setValues(listLog);
		lokiStreamList.add(lokiStream);
		lokiStreams.setStreams(lokiStreamList);
		String actualLogString = lokiStreams.toJsonString();
		Assertions.assertEquals(expectedLog, actualLogString);
	}

	@Test
	public void testCanSetLogMessage() throws JsonProcessingException {
		Map<String, String> labels = new HashMap<>();
		LokiLog lokiLog = new LokiLog();
		lokiLog.setLogMessage(LogLevel.INFO.value(), "Change Log Data");
		String expectedLog = String.format(
				"{\"streams\":[{\"stream\":{\"test_label\":\"labels-value\"},\"values\":[[\"%d\",\"[INFO] This is test info log\"],[\"%d\",\"[WARN] This is test warn log\"],[\"%d\",\"%s\"]]}]}",
				infoLog.getUnixEpochNano(), warnLog.getUnixEpochNano(), lokiLog.getUnixEpochNano(),lokiLog.getLogMessage());
		labels.put("test_label", "labels-value");
		LokiStreams lokiStreams = new LokiStreams();
		List<LokiStream> lokiStreamList = new ArrayList<>();
		List<List<String>> listLog = new ArrayList<>();
		LokiStream lokiStream = new LokiStream();
		lokiStream.setStream(labels);
		listLog.add(infoLog.getLogObject());
		listLog.add(warnLog.getLogObject());
		listLog.add(lokiLog.getLogObject());
		lokiStream.setValues(listLog);
		lokiStreamList.add(lokiStream);
		lokiStreams.setStreams(lokiStreamList);
		String actualLogString = lokiStreams.toJsonString();
		Assertions.assertEquals(expectedLog, actualLogString);
	}
	
	@Test
	public void testLokiResponseDto() throws InterruptedException, ExecutionException {
		this.lokiMockServer.stubLokiPushLogAPI("[INFO] Stub Log Data", 200);
		CompletableFuture<LokiResponse> future = new CompletableFuture<>();
		LokiDBClient client = new LokiDBClient(this.sendLogThreadPool, this.httpClientThreadPool);
		client.createLokiClient(this.getLokiHttpMockServerUrl(), 3000, 3000);
		client.sendAsync("Hello".getBytes()).thenAccept((r) -> {
			future.complete(r);
		});
		future.join();
		Assertions.assertEquals(200, future.get().getStatus());
		Assertions.assertEquals("[INFO] Stub Log Data", future.get().getBody());
		client.stopLokiClient(1, 1);
	}

}
