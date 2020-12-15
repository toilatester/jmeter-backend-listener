package toilatester.jmeter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import toilatester.jmeter.utils.LokiMockServer;

public abstract class BaseTest {

	protected LokiMockServer lokiMockServer;
	@BeforeEach
	public void beforeEach() {
		
	}
	
	@AfterEach
	public void afterEach() {}
}
