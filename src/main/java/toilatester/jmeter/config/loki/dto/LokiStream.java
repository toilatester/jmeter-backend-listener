package toilatester.jmeter.config.loki.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LokiStream {

	private Map<String, String> stream;
	private List<List<String>> values;

	public Map<String, String> getStream() {
		return stream;
	}

	public void setStream(Map<String, String> lokiLabels) {
		this.stream = normalizeLabels(lokiLabels);
	}

	public List<List<String>> getValues() {
		return values;
	}

	public void setValues(List<List<String>> values) {
		this.values = values;
	}

	private Map<String, String> normalizeLabels(Map<String, String> lokiLabels) {
		Map<String, String> normalizeLabels = new HashMap<String, String>();
		for (Entry<String, String> labelSet : lokiLabels.entrySet()) {
			String rawLabel = labelSet.getKey();
			String lokiLabel = rawLabel.strip().trim().replaceAll("(\\W)", "");
			normalizeLabels.put(lokiLabel, labelSet.getValue());
		}
		return normalizeLabels;
	}
}
