# Findings: Gemini extraction on real syllabi

Tested on 2 syllabus so far (target: 3-5), Oct 29, 2026.
Model: gemini-3.5-flash-lite.
Term start and end dates were entered manually for each run.

Counting rules: only items with an explicit date or a stated weekly pattern count.
Categories are tracked separately because they fail in different ways: explicit dates,
table-derived dates (week row + weekday column), administrative dates, recurring
events, and relative dates ("Week N" with no date range).

## Results

| Course | Category | In syllabus | Correct | Missed | Wrong date | Notes |
|---|---|---|---|---|---|---|
| MATH 125 | Explicit dates | 2 | 2 | 0 | 0 | Midterm and final: date, start time and time zone correct |
| MATH 125 | Recurring | 2 | 0 | 2 | n/a | Weekly quiz and weekly homework produced no events |
| MATH 125 | Relative dates | 0 | n/a | n/a | n/a | Syllabus has no "Week N" dates |
| MATH 114 | Explicit dates | 1 | 1 | 0 | 0 | Final exam: date, 7:30 start and time zone correct; 10:15 end time lost |
| MATH 114 | Table-derived | 2 | 2 | 0 | 0 | Exams 1 and 2 read from a week-by-week table; weights (20%) correct |
| MATH 114 | Administrative dates | 3 | 0 | 3 | n/a | Three drop deadlines not extracted (scope decision pending) |
| MATH 114 | Recurring | 0 | n/a | n/a | n/a | No stated weekly deadline pattern |
| MATH 114 | Relative dates | 0 | n/a | n/a | n/a | Every week row in the table includes its date range |
| Course C | | | | | | |

## Failure patterns

- **Recurring events are dropped.** MATH 125 says quizzes happen every Thursday with no
  dates listed; the model returned nothing. The pipeline has no recurrence support.
- **End time is lost.** Both syllabi give a time range (10:30-11:30, 7:30-10:15), but the
  event record has no end time field, so the calendar event has only a start.
- **Administrative dates are skipped.** MATH 114 lists three drop deadlines. The model did
  not extract them. Whether this is a miss depends on whether the product should cover
  university deadlines or only assignments and exams.
- **Table-style schedules:** handled correctly in 1 syllabus (2 of 2 events). Too small a
  sample to conclude anything; the table was clean and each week row carried its date range.
- **Relative dates ("Week 5 Friday"):** not observed yet. Neither syllabus requires the
  model to compute a date from a bare week number, so RelativeDateResolver is untested
  against real data.
- **Hallucinated dates:** none observed. The model left weight empty for MATH 125, where
  the syllabus uses a "best of two weightings" rule.
- **Duplicates:** not observed yet.


## What I changed because of this

- [Only if true: timed events now use the term time zone (America/New_York)
  instead of UTC.]
  
## Open problems

- Add end time and location fields to the extracted event.
- Support recurring events (generate from the term range, or use RRULE).
- Decide whether administrative dates (drop deadlines) are in scope.
- Exams with no stated time are exported as all-day events. MATH 114 gives only the class
  meeting time (MW 8:30-10:20); decide whether to use it as the default exam time.
- Test syllabi that use bare "Week N" dates before deciding whether
  RelativeDateResolver should own the date calculation.