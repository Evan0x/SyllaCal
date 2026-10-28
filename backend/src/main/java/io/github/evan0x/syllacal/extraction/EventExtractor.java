package io.github.evan0x.syllacal.extraction;

import java.time.LocalDate;
import java.util.List;

public interface EventExtractor {

	List<ExtractedEvent> extract(String syllabusText, LocalDate termStart, LocalDate termEnd);
}
