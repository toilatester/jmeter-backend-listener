package toilatester.jmeter.config.influxdb.measurement;

import toilatester.jmeter.report.exception.ReportException;

/**
 * Constants (Tag, Field, Measurement) names for the virtual users measurement.
 * 
 * @author MinhHoang
 *
 */
public class VirtualUsersMeasurement {
	private VirtualUsersMeasurement() {
		throw new ReportException("Cannot init Constant class");
	}

	/**
	 * Measurement name.
	 */
	public static final String MEASUREMENT_NAME = "virtualUsers";

	/**
	 * Tags.
	 * 
	 * @author MinhHoang
	 *
	 */
	public class Tags {
		private Tags() {
			throw new ReportException("Constant class");
		}

		/**
		 * Node name field
		 */
		public static final String NODE_NAME = "nodeName";
	}

	/**
	 * Fields.
	 * 
	 * @author MinhHoang
	 *
	 */
	public class Fields {
		private Fields() {
			throw new ReportException("Constant class");
		}

		/**
		 * Minimum active threads field.
		 */
		public static final String MIN_ACTIVE_THREADS = "minActiveThreads";
		/**
		 * Maximum active threads field.
		 */
		public static final String MAX_ACTIVE_THREADS = "maxActiveThreads";

		/**
		 * Mean active threads field.
		 */
		public static final String MEAN_ACTIVE_THREADS = "meanActiveThreads";

		/**
		 * Started threads field.
		 */
		public static final String STARTED_THREADS = "startedThreads";

		/**
		 * Finished threads field.
		 */
		public static final String FINISHED_THREADS = "finishedThreads";
	}
}
