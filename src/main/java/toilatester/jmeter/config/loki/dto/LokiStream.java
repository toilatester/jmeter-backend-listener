package toilatester.jmeter.config.loki.dto;

import java.util.List;
import java.util.Map;

public class LokiStream {

	private Map<String, String> stream;
	private List<List<String>> values;

	public Map<String, String> getStream() {
		return stream;
	}

	public void setStream(Map<String, String> stream) {
		this.stream = stream;
	}

	public List<List<String>> getValues() {
		return values;
	}

	public void setValues(List<List<String>> values) {
		this.values = values;
	}
}
