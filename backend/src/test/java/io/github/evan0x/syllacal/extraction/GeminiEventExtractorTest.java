package io.github.evan0x.syllacal.extraction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiEventExtractorTest {

	private GeminiEventExtractor extractor;

	@BeforeEach
	void setUp() {
		extractor = new GeminiEventExtractor(JsonMapper.builder().build(), "unused prompt");
	}

	@Test
	void parseEventsJsonMapsSnakeCaseFields() {
		String json = """
				{
				  "events": [
				    {
				      "title": "Exam 1",
				      "type": "EXAM",
				      "date": "2026-09-30",
				      "time": "08:30",
				      "weight_percent": 20,
				      "confidence": "HIGH",
				      "source_text": "Exam 1 (Ch 6-7.4)"
				    },
				    {
				      "title": "Participation",
				      "type": "OTHER",
				      "date": null,
				      "time": null,
				      "weight_percent": 10,
				      "confidence": "LOW",
				      "source_text": "Participation - 10%"
				    }
				  ]
				}
				""";

		List<ExtractedEvent> events = extractor.parseEventsJson(json);

		assertEquals(2, events.size());
		ExtractedEvent exam = events.get(0);
		assertEquals("Exam 1", exam.title());
		assertEquals("EXAM", exam.type());
		assertEquals(LocalDate.of(2026, 9, 30), exam.date());
		assertEquals(LocalTime.of(8, 30), exam.time());
		assertEquals(20.0, exam.weightPercent());
		assertEquals("HIGH", exam.confidence());
		assertEquals("Exam 1 (Ch 6-7.4)", exam.sourceText());

		ExtractedEvent participation = events.get(1);
		assertNull(participation.date());
		assertNull(participation.time());
		assertEquals(10.0, participation.weightPercent());
	}

	@Test
	void parseEventsJsonThrowsWhenNotValidJson() {
		ExtractionException ex = assertThrows(ExtractionException.class,
				() -> extractor.parseEventsJson("not-json"));

		assertTrue(ex.getMessage().toLowerCase().contains("json"));
	}

	@Test
	void parseEventsJsonThrowsWhenSchemaMissingEvents() {
		assertThrows(ExtractionException.class,
				() -> extractor.parseEventsJson("{\"items\":[]}"));
	}
}
