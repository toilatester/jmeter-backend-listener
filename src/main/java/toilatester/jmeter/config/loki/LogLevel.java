package toilatester.jmeter.config.loki;

public enum LogLevel {

	INFO("INFO"), WARN("WARN"), ERROR("ERROR");

	private String logLevel;

	private LogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public static LogLevel getLogLevelByString(String logLevel) {
		for (LogLevel level : LogLevel.values()) {
			if (level.logLevel.equals(logLevel))
				return level;
		}
		throw new EnumConstantNotPresentException(LogLevel.class, logLevel);
	}

	public String value() {
		return this.logLevel;
	}

}
