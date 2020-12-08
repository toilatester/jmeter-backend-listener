package toilatester.jmeter.config.loki.dto;

import java.util.Arrays;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

	@SuppressWarnings("serial")
	public static void main(String[] args) throws JsonProcessingException {
		LokiStreams lokiStreams = new LokiStreams();
		LokiStream lokiStream = new LokiStream();

		lokiStream.setValues(Arrays.asList(new LokiLog("Sample log 1").getLogObject(),
				new LokiLog("Sample log 2").getLogObject(), new LokiLog("Sample log 3").getLogObject()));
		lokiStream.setStream(new HashMap<String, String>() {
			{
				put("jmeter plugin-123_345", "toilatester");
				put("external_label", "minhhoang");
			}
		});
		lokiStreams.setStreams(Arrays.asList(lokiStream));
		ObjectMapper mapper = new ObjectMapper();
		String requestJSON = mapper.writeValueAsString(lokiStreams);
		System.out.println(requestJSON);
	}

}
