package io.github.evan0x.syllacal.calendar;

import io.github.evan0x.syllacal.extraction.ExtractedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventValidatorTest {

	private final EventValidator validator = new EventValidator();
	private final LocalDate termStart = LocalDate.of(2026, 8, 24);
	private final LocalDate termEnd = LocalDate.of(2026, 12, 16);

	@Test
	void dropsEventsWithNullDate() {
		ExtractedEvent withDate = event("Exam 1", LocalDate.of(2026, 9, 30), "HIGH");
		ExtractedEvent noDate = event("Homework", null, "MEDIUM");

		ValidationResult result = validator.validate(List.of(withDate, noDate), termStart, termEnd);

		assertEquals(1, result.validEvents().size());
		assertEquals("Exam 1", result.validEvents().getFirst().title());
		assertTrue(result.warnings().stream().anyMatch(w -> w.contains("null date")));
	}

	@Test
	void flagsButKeepsOutOfRangeDates() {
		ExtractedEvent outside = event("Summer Work", LocalDate.of(2026, 7, 1), "HIGH");

		ValidationResult result = validator.validate(List.of(outside), termStart, termEnd);

		assertEquals(1, result.validEvents().size());
		assertTrue(result.warnings().stream().anyMatch(w -> w.contains("outside term range")));
	}

	@Test
	void mergesExactDuplicatesByTitleAndDate() {
		ExtractedEvent first = event("Exam 1", LocalDate.of(2026, 9, 30), "HIGH");
		ExtractedEvent duplicate = event("Exam 1", LocalDate.of(2026, 9, 30), "MEDIUM");

		ValidationResult result = validator.validate(List.of(first, duplicate), termStart, termEnd);

		assertEquals(1, result.validEvents().size());
		assertEquals("HIGH", result.validEvents().getFirst().confidence());
		assertTrue(result.warnings().stream().anyMatch(w -> w.contains("duplicate")));
	}

	@Test
	void keepsLowConfidenceButMarksThem() {
		ExtractedEvent low = event("Quiz", LocalDate.of(2026, 10, 1), "LOW");

		ValidationResult result = validator.validate(List.of(low), termStart, termEnd);

		assertEquals(1, result.validEvents().size());
		assertTrue(result.warnings().stream().anyMatch(w -> w.contains("LOW confidence")));
	}

	private static ExtractedEvent event(String title, LocalDate date, String confidence) {
		return new ExtractedEvent(title, "EXAM", date, null, 20.0, confidence, "source");
	}
}
