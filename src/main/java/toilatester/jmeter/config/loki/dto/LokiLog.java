package toilatester.jmeter.config.loki.dto;

import java.util.Arrays;
import java.util.List;

public class LokiLog {

	private static final int ONE_MS_IN_NANOSECONDS = 1000000;
	List<String> logObject;
	long unixEpochNano;
	String logMessage = "";

	public LokiLog(String logLevel, String logMessage) {
		this.unixEpochNano = System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS;
		this.setLogMessage(logLevel, logMessage);
	}

	public LokiLog() {
		this.unixEpochNano = System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS;
	}

	public List<String> getLogObject() {
		return Arrays.asList(Long.toString(this.unixEpochNano), this.logMessage);
	}

	public long getUnixEpochNano() {
		return unixEpochNano;
	}

	public String getLogMessage() {
		return logMessage;
	}

	public void setLogMessage(String logLevel, String logMessage) {
		this.logMessage = String.format("[%s] %s", logLevel, logMessage);
	}
}
