package toilatester.jmeter.config.influxdb;

import toilatester.jmeter.report.exception.ReportException;

/**
 * Constants (Tag, Field, Measurement) names for the requests measurement.
 * 
 * @author minhhoang 
 *
 */
public class RequestMeasurement {
	private RequestMeasurement() {
		throw new ReportException("Cannot init Constant class");
	}

	/**
	 * Measurement name.
	 */
	public static final String MEASUREMENT_NAME = "requestsRaw";

	/**
	 * Tags.
	 * 
	 * @author minhhoang	
	 *
	 */
	public class Tags {
		private Tags() {
			throw new ReportException("Constant class");
		}

		/**
		 * Request name tag.
		 */
		public static final String REQUEST_NAME = "requestName";
	}

	/**
	 * Fields.
	 * 
	 * @author minhhoang	
	 *
	 */
	public class Fields {
		private Fields() {
			throw new ReportException("Constant class");
		}

		/**
		 * Response time field.
		 */
		public static final String RESPONSE_TIME = "responseTime";

		/**
		 * Error count field.
		 */
		public static final String ERROR_COUNT = "errorCount";

		/**
		 * Thread name field
		 */
		public static final String THREAD_NAME = "threadName";

		/**
		 * Latency
		 */
		public static final String LATENCY_TIME = "latencyTime";

		/**
		 * Test name field
		 */
		public static final  String TEST_NAME = "testName";

		/**
		 * Node name field
		 */
		public static final String NODE_NAME = "nodeName";
	}
}
