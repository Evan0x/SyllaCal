package io.github.evan0x.syllacal.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExtractedEvent(
		String title,
		String type,
		LocalDate date,
		LocalTime time,
		@JsonProperty("weight_percent") Double weightPercent,
		String confidence,
		@JsonProperty("source_text") String sourceText
) {
}
