package io.github.evan0x.syllacal.web;

import io.github.evan0x.syllacal.calendar.EventValidator;
import io.github.evan0x.syllacal.calendar.IcsBuilder;
import io.github.evan0x.syllacal.calendar.ValidationResult;
import io.github.evan0x.syllacal.extraction.EventExtractor;
import io.github.evan0x.syllacal.extraction.ExtractedEvent;
import io.github.evan0x.syllacal.pdf.PdfTextExtractor;
import io.github.evan0x.syllacal.pdf.ScannedPdfException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
public class SyllabusController {

	static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

	private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

	private final PdfTextExtractor pdfTextExtractor;
	private final EventExtractor eventExtractor;
	private final EventValidator eventValidator = new EventValidator();
	private final IcsBuilder icsBuilder = new IcsBuilder();

	public SyllabusController(PdfTextExtractor pdfTextExtractor, EventExtractor eventExtractor) {
		this.pdfTextExtractor = pdfTextExtractor;
		this.eventExtractor = eventExtractor;
	}

	@PostMapping("/api/syllabus")
	public ResponseEntity<String> createCalendar(
			@RequestParam("file") MultipartFile file,
			@RequestParam("courseName") String courseName,
			@RequestParam("termStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate termStart,
			@RequestParam("termEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate termEnd)
			throws ScannedPdfException, IOException {
		String course = requireCourseName(courseName);
		requireTerm(termStart, termEnd);
		byte[] pdf = requirePdf(file);

		String syllabusText;
		try {
			syllabusText = pdfTextExtractor.extract(new ByteArrayInputStream(pdf));
		}
		catch (IOException exception) {
			throw new SyllabusRequestException(HttpStatus.BAD_REQUEST, pdfReadMessage(exception));
		}

		List<ExtractedEvent> extracted = eventExtractor.extract(syllabusText, termStart, termEnd);
		ValidationResult validated = eventValidator.validate(extracted, termStart, termEnd);
		String ics = icsBuilder.build(validated.validEvents(), course);

		ContentDisposition disposition = ContentDisposition.attachment()
				.filename(filename(course), StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/calendar"))
				.header("Content-Disposition", disposition.toString())
				.body(ics);
	}

	private static String requireCourseName(String courseName) {
		if (courseName == null || courseName.isBlank()) {
			throw new SyllabusRequestException(HttpStatus.BAD_REQUEST, "courseName is required");
		}
		return courseName.trim();
	}

	private static void requireTerm(LocalDate termStart, LocalDate termEnd) {
		if (termEnd.isBefore(termStart)) {
			throw new SyllabusRequestException(HttpStatus.BAD_REQUEST, "termEnd must be on or after termStart");
		}
	}

	private static byte[] requirePdf(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new SyllabusRequestException(HttpStatus.BAD_REQUEST, "A PDF file is required");
		}
		if (file.getSize() > MAX_FILE_BYTES) {
			throw new SyllabusRequestException(HttpStatus.BAD_REQUEST, "File exceeds the 10 MB limit");
		}
		byte[] bytes = file.getBytes();
		if (!isPdf(bytes)) {
			throw new SyllabusRequestException(HttpStatus.BAD_REQUEST, "File is not a PDF");
		}
		return bytes;
	}

	static boolean isPdf(byte[] bytes) {
		if (bytes == null || bytes.length < PDF_MAGIC.length) {
			return false;
		}
		for (int i = 0; i < PDF_MAGIC.length; i++) {
			if (bytes[i] != PDF_MAGIC[i]) {
				return false;
			}
		}
		return true;
	}

	private static String pdfReadMessage(IOException exception) {
		String message = exception.getMessage();
		if (message != null && message.toLowerCase().contains("encrypt")) {
			return "Encrypted PDFs are not supported";
		}
		return "The file is not a readable PDF";
	}

	private static String filename(String courseName) {
		String safe = courseName.replaceAll("[^A-Za-z0-9._-]+", "_");
		if (safe.isBlank()) {
			safe = "syllabus";
		}
		return safe + ".ics";
	}
}
