package io.github.evan0x.syllacal.calendar;

import io.github.evan0x.syllacal.extraction.ExtractedEvent;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds an iCalendar (.ics) document from extracted events.
 *
 * <p>Uses ical4j 4.x Temporal API: {@link LocalDate} for all-day DATE values and
 * {@link ZonedDateTime} for DATE-TIME values with a TZID. See
 * <a href="https://github.com/ical4j/ical4j">ical4j README</a> and
 * <a href="https://www.ical4j.org/examples/model/">model examples</a>.
 */
public class IcsBuilder {

	public String build(List<ExtractedEvent> events, String courseName) {
		return build(events, courseName, ZoneId.of("UTC"));
	}

	public String build(List<ExtractedEvent> events, String courseName, ZoneId timeZone) {
		Objects.requireNonNull(events, "events");
		Objects.requireNonNull(courseName, "courseName");
		Objects.requireNonNull(timeZone, "timeZone");

		Calendar calendar = new Calendar();
		calendar.add(new ProdId("-//SyllaCal//EN"));
		calendar.add(ImmutableVersion.VERSION_2_0);
		calendar.add(ImmutableCalScale.GREGORIAN);

		for (ExtractedEvent event : events) {
			if (event.date() == null) {
				continue;
			}
			calendar.add(toVEvent(event, courseName, timeZone));
		}

		return calendar.toString();
	}

	private static VEvent toVEvent(ExtractedEvent event, String courseName, ZoneId timeZone) {
		String summary = event.title() == null ? courseName : event.title();
		VEvent vevent;
		if (event.time() == null) {
			vevent = new VEvent(event.date(), summary);
		}
		else {
			ZonedDateTime start = ZonedDateTime.of(event.date(), event.time(), timeZone);
			vevent = new VEvent(start, summary);
		}
		vevent.add(new Uid(stableUid(courseName, event.title(), event.date())));
		vevent.add(new Description(buildDescription(event)));
		return vevent;
	}

	static String stableUid(String courseName, String title, LocalDate date) {
		String material = Objects.toString(courseName, "") + "|"
				+ Objects.toString(title, "") + "|"
				+ Objects.toString(date, "");
		return UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)) + "@syllacal";
	}

	private static String buildDescription(ExtractedEvent event) {
		StringBuilder description = new StringBuilder();
		if (event.weightPercent() != null) {
			double weight = event.weightPercent();
			String weightText = weight == Math.rint(weight)
					? Long.toString(Math.round(weight))
					: Double.toString(weight);
			description.append("Weight: ").append(weightText).append('%');
		}
		if (event.sourceText() != null && !event.sourceText().isBlank()) {
			if (!description.isEmpty()) {
				description.append('\n');
			}
			description.append("Source: ").append(event.sourceText());
		}
		return description.toString();
	}
}
