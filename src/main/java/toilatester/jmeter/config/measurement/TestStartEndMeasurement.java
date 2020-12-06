package toilatester.jmeter.config.measurement;

import toilatester.jmeter.report.exception.ReportException;

/**
 * Constants (Tag, Field, Measurement) names for the measurement that denotes
 * start and end points of a load test.
 * 
 * @author minhhoang
 *
 */
public class TestStartEndMeasurement {
	private TestStartEndMeasurement() {
		throw new ReportException("Cannot init Constant class");
	}

	/**
	 * Measurement name.
	 */
	public static final String MEASUREMENT_NAME = "testStartEnd";

	/**
	 * Tags.
	 * 
	 * @author minhhoang
	 *
	 */
	public class Tags {
		private Tags() {
			throw new ReportException("Tags Constant class");
		}

		/**
		 * Start or End type tag.
		 */
		public static final String TYPE = "type";

		/**
		 * Node name field
		 */
		public static final String NODE_NAME = "nodeName";
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
		 * Test name field.
		 */
		public static final String TEST_NAME = "testName";
	}

	/**
	 * Values.
	 * 
	 * @author minhhoang
	 *
	 */
	public class Values {
		private Values() {
			throw new ReportException("Constant class");
		}

		/**
		 * Finished.
		 */
		public static final String FINISHED = "finished";
		/**
		 * Started.
		 */
		public static final String STARTED = "started";
	}
}
