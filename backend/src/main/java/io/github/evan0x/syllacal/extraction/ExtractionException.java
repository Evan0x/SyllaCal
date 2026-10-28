package io.github.evan0x.syllacal.extraction;

public class ExtractionException extends RuntimeException {

	public ExtractionException(String message) {
		super(message);
	}

	public ExtractionException(String message, Throwable cause) {
		super(message, cause);
	}
}
