package io.github.evan0x.syllacal.term;

import java.time.LocalDate;
import java.util.Optional;


public class RelativeDateResolver {
        /**
     * Converts relative date text found in course syllabi into a concrete date.
     *
     * <p>Supported formats (case-insensitive, extra spaces and commas allowed):
     * <ul>
     *   <li>"Week 5 Friday"</li>
     *   <li>"Wk 5 Fri"</li>
     *   <li>"Week 5, Friday"</li>
     *   <li>"Friday of Week 5"</li>
     * </ul>
     *
     * <p>Week rules: each week starts on Monday and ends on Sunday. Instructional
     * week N is the Nth calendar week of the term that is not skipped
     * (see {@link Term#skippedCalendarWeeks()}).
     *
     * <p>Returns {@link Optional#empty()} when: the text is null or blank, the text
     * is not recognized, the week number is less than 1, the week number is beyond
     * the end of the term, or the resulting date falls outside
     * [firstDay, lastDay]. Never throws an exception for bad input.
     *
     * @param text the original text, e.g. "Week 5 Friday"
     * @param term the term calendar information
     * @return the concrete date, or empty if the text cannot be resolved
     */
    public Optional<LocalDate> resolve(String text, Term term) {
        throw new UnsupportedOperationException("TODO");
    }
}
