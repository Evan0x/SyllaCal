package io.github.evan0x.syllacal.calendar;

import io.github.evan0x.syllacal.extraction.ExtractedEvent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EventValidator {

	public ValidationResult validate(List<ExtractedEvent> events, LocalDate termStart, LocalDate termEnd) {
		Objects.requireNonNull(events, "events");
		Objects.requireNonNull(termStart, "termStart");
		Objects.requireNonNull(termEnd, "termEnd");

		List<String> warnings = new ArrayList<>();
		Map<String, ExtractedEvent> byTitleAndDate = new LinkedHashMap<>();

		for (ExtractedEvent event : events) {
			if (event.date() == null) {
				warnings.add("Dropped event with null date: " + safeTitle(event));
				continue;
			}

			if (event.date().isBefore(termStart) || event.date().isAfter(termEnd)) {
				warnings.add("Event outside term range [" + termStart + ", " + termEnd + "]: "
						+ safeTitle(event) + " on " + event.date());
			}

			if ("LOW".equalsIgnoreCase(event.confidence())) {
				warnings.add("LOW confidence event kept: " + safeTitle(event) + " on " + event.date());
			}

			String key = safeTitle(event) + "|" + event.date();
			if (byTitleAndDate.containsKey(key)) {
				warnings.add("Merged duplicate event: " + safeTitle(event) + " on " + event.date());
				continue;
			}
			byTitleAndDate.put(key, event);
		}

		return new ValidationResult(List.copyOf(byTitleAndDate.values()), List.copyOf(warnings));
	}

	private static String safeTitle(ExtractedEvent event) {
		return event.title() == null ? "(untitled)" : event.title();
	}
}
