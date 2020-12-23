/**
 * 
 */
package toilatester.jmeter.report.exception;

/**
 * @author minhhoang
 *
 */
public class ReportException extends RuntimeException{
	private static final long serialVersionUID = 0L;

	public ReportException(String message) {
		super(message);
	}

	public ReportException(String message, Throwable cause) {
		super(message, cause);
	}
}
