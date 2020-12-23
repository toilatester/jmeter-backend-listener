package toilatester.jmeter.config.loki;

public enum LogLevel {

	INFO("INFO"), WARN("WARN"), ERROR("ERROR");

	private String logLevel;

	private LogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public String value() {
		return this.logLevel;
	}

}
