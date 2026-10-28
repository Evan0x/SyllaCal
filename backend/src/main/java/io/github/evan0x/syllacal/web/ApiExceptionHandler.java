package io.github.evan0x.syllacal.web;

import io.github.evan0x.syllacal.extraction.ExtractionException;
import io.github.evan0x.syllacal.pdf.ScannedPdfException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(SyllabusRequestException.class)
	public ResponseEntity<ApiError> handleRequest(SyllabusRequestException exception) {
		return ResponseEntity.status(exception.getStatus()).body(new ApiError(exception.getMessage()));
	}

	@ExceptionHandler(ScannedPdfException.class)
	public ResponseEntity<ApiError> handleScanned(ScannedPdfException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ApiError(exception.getMessage()));
	}

	@ExceptionHandler(ExtractionException.class)
	public ResponseEntity<ApiError> handleExtraction(ExtractionException exception) {
		String message = exception.getMessage() == null
				? "Could not extract events from the syllabus"
				: exception.getMessage();
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ApiError(message));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException exception) {
		return ResponseEntity.badRequest().body(new ApiError("File exceeds the 10 MB limit"));
	}

	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MissingServletRequestPartException.class,
			MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ApiError> handleBadInput(Exception exception) {
		return ResponseEntity.badRequest().body(new ApiError(badInputMessage(exception)));
	}

	private static String badInputMessage(Exception exception) {
		if (exception instanceof MissingServletRequestPartException missing && "file".equals(missing.getRequestPartName())) {
			return "A PDF file is required";
		}
		if (exception instanceof MissingServletRequestParameterException missing) {
			return missing.getParameterName() + " is required";
		}
		if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
			return mismatch.getName() + " must be a date in YYYY-MM-DD format";
		}
		return "Invalid request";
	}
}
