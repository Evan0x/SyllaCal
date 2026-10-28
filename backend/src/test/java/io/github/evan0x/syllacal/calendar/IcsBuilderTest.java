package io.github.evan0x.syllacal.calendar;

import io.github.evan0x.syllacal.extraction.ExtractedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IcsBuilderTest {

	private final IcsBuilder builder = new IcsBuilder();
	private final ZoneId zone = ZoneId.of("America/New_York");

	@Test
	void buildsAllDayEventWithDateValue() {
		ExtractedEvent event = new ExtractedEvent(
				"Exam 1", "EXAM", LocalDate.of(2026, 9, 30), null,
				20.0, "HIGH", "Exam 1 (Ch 6-7.4)");

		String ics = builder.build(List.of(event), "MATH 101", zone);

		assertTrue(ics.contains("DTSTART;VALUE=DATE:20260930"));
		assertFalse(ics.contains("T083000"));
		assertTrue(ics.contains("SUMMARY:Exam 1"));
		assertTrue(ics.contains("Weight: 20%"), () -> "ICS was:\n" + ics);
		assertTrue(ics.contains("Source: Exam 1 (Ch 6-7.4)"), () -> "ICS was:\n" + ics);
	}

	@Test
	void buildsTimedEventWithTimeZone() {
		ExtractedEvent event = new ExtractedEvent(
				"Exam 2", "EXAM", LocalDate.of(2026, 11, 4), LocalTime.of(8, 30),
				20.0, "HIGH", "Exam 2");

		String ics = builder.build(List.of(event), "MATH 101", zone);

		assertTrue(ics.contains("DTSTART;TZID=America/New_York:20261104T083000"));
		assertFalse(ics.contains("VALUE=DATE:20261104"));
	}

	@Test
	void uidIsStableAcrossBuilds() {
		ExtractedEvent event = new ExtractedEvent(
				"Final", "EXAM", LocalDate.of(2026, 12, 14), LocalTime.of(7, 30),
				25.0, "HIGH", "Final Exam");

		String first = builder.build(List.of(event), "MATH 101", zone);
		String second = builder.build(List.of(event), "MATH 101", zone);
		String expectedUid = IcsBuilder.stableUid("MATH 101", "Final", LocalDate.of(2026, 12, 14));

		assertTrue(first.contains("UID:" + expectedUid));
		assertEquals(uidLine(first), uidLine(second));
	}

	@Test
	void differentCourseOrTitleProducesDifferentUid() {
		LocalDate date = LocalDate.of(2026, 12, 14);
		String uidA = IcsBuilder.stableUid("MATH 101", "Final", date);
		String uidB = IcsBuilder.stableUid("CS 101", "Final", date);
		String uidC = IcsBuilder.stableUid("MATH 101", "Midterm", date);

		assertFalse(uidA.equals(uidB));
		assertFalse(uidA.equals(uidC));
	}

	private static String uidLine(String ics) {
		return ics.lines()
				.filter(line -> line.startsWith("UID:"))
				.findFirst()
				.orElseThrow();
	}
}
