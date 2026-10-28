package io.github.evan0x.syllacal.web;

import org.springframework.http.HttpStatus;

public class SyllabusRequestException extends RuntimeException {

	private final HttpStatus status;

	public SyllabusRequestException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
