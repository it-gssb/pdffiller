package org.gssb.pdffiller.exception;

public class UnrecoverableException extends RuntimeException {

	private static final long serialVersionUID = 3832844785771986173L;

	public UnrecoverableException(final String message) {
	    super(message);
	 }
	
	public UnrecoverableException(final String message, final Throwable t) {
		super(message, t);
	}
}
