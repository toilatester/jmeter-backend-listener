/**
 * 
 */
package toilatester.jmeter.config.measurement;

import toilatester.jmeter.report.exception.ReportException;

/**
 * @author minhhoang
 *
 */
public class ConnectMeasurement {

	private ConnectMeasurement() {
		throw new ReportException("Cannot init Constant class");
	}

	/**
	 * Measurement name.
	 */
	public static final String MEASUREMENT_NAME = "connectRaw";

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
		
		public static final String SAMPLE_NAME = "sampleName";
		public static final String START_TIME = "stratTime";
		public static final String END_TIME = "endTime";
		public static final String RESPONSE_SIZE = "responseSize";
		public static final String CONNECT_TIME = "connectTime";
		public static final String SEND_BYTE = "sendByte";
	}
}
