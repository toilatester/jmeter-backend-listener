package toilatester.jmeter.config.loki.dto;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LokiStreams {

	private List<LokiStream> streams;

	public List<LokiStream> getStreams() {
		return streams;
	}

	public void setStreams(List<LokiStream> streams) {
		this.streams = streams;
	}
	
	public String toJsonString() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
}
