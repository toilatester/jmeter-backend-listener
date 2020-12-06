/**
 * 
 */
package toilatester.jmeter.config.influxdb.measurement;

import toilatester.jmeter.report.exception.ReportException;

/**
 * @author minhhoang
 *
 */
public class ErrorMeasurement {
	private ErrorMeasurement() {
		throw new ReportException("Cannot Init Constant class");
	}

	/**
	 * Measurement name.
	 */
	public static final String MEASUREMENT_NAME = "errorsRaw";

	/**
	 * Tags.
	 * 
	 * @extend by MinhHoang
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
	 * @extend by MinhHoang
	 *
	 */
	public class Fields {
		private Fields() {
			throw new ReportException("Constant class");
		}

		/**
		 * Error count field.
		 */
		public static final String ERROR_COUNT = "errorCount";

		/**
		 * Response data in error
		 */
		public static final String RESPONSE_CODE = "responseCode";

		/**
		 * Response data in error
		 */
		public static final String RESPONSE_MESSAGE = "responseMessage";

		/**
		 * Response body
		 */
		public static final String RESPONSE_BODY = "responseBody";

		/**
		 * Assert Data
		 */
		public static final String ASSERT_DATA = "assertData";

	}
}
