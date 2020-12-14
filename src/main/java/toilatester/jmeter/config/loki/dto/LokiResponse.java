package toilatester.jmeter.config.loki.dto;

public class LokiResponse {
	private int status;
	private String body;

	public LokiResponse(int status, String body) {
		this.setStatus(status);
		this.setBody(body);
	}

	public LokiResponse() {
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	
}
