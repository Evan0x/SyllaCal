package io.github.evan0x.syllacal.calendar;

import io.github.evan0x.syllacal.extraction.ExtractedEvent;

import java.util.List;

public record ValidationResult(
		List<ExtractedEvent> validEvents,
		List<String> warnings
) {
}
