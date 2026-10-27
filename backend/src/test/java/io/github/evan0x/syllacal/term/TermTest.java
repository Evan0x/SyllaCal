package io.github.evan0x.syllacal.term;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermTest {

    @Test
    void overlapsReturnsTrueWhenDateRangesIntersect() {
        Term fall = new Term("Fall Semester", LocalDate.of(2025, 8, 25), LocalDate.of(2025, 12, 18));
        Term finals = new Term("Finals Week", LocalDate.of(2025, 12, 10), LocalDate.of(2025, 12, 20));

        assertTrue(fall.overlaps(finals));
    }

    @Test
    void containsReturnsTrueOnlyForDatesInsideTheTerm() {
        Term spring = new Term("Spring Semester", LocalDate.of(2025, 1, 13), LocalDate.of(2025, 5, 9));

        assertTrue(spring.contains(LocalDate.of(2025, 2, 14)));
        assertFalse(spring.contains(LocalDate.of(2025, 12, 1)));
    }

    @Test
    void getDurationInDaysCountsWholeTermLength() {
        Term summer = new Term("Summer Term", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 10));

        assertEquals(9, summer.getDurationInDays());
    }
}
