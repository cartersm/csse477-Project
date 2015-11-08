package protocol.plugin;

import java.util.NoSuchElementException;

/**
 * A basic Exception for handling errors in which the servlet cannot be found.
 * 
 */
public final class ServletUndefinedException extends NoSuchElementException {
	private static final long serialVersionUID = -4473656847785419240L;

	public ServletUndefinedException(String message) {
		super(message);
	}
}