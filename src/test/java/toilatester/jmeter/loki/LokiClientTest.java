package toilatester.jmeter.loki;

import org.junit.jupiter.api.Test;

import toilatester.jmeter.utils.LokiMockServer;


public class LokiClientTest {

	
	@Test
	public void testClientCanConnect() {
		LokiMockServer lokiMockServer = new LokiMockServer();
		lokiMockServer.startServer();
		System.err.println(lokiMockServer.stubResponseData());
		lokiMockServer.stopServer();
	}

}
