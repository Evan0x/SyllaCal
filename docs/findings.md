# Findings: Gemini extraction on real syllabi

Tested on 1 syllabus so far (target: 3-5), Oct 29, 2026.
Model: [copy the exact name from application.yml].
Term start and end dates were entered manually for each run.

Counting rules: only items with an explicit date or a stated weekly pattern count.
Three categories are tracked separately because they fail in different ways:
explicit dates, recurring events, and relative dates ("Week N").

## Results

| Course | Category | In syllabus | Correct | Missed | Wrong date | Notes |
|---|---|---|---|---|---|---|
| MATH 125 | Explicit dates | 2 | 2 | 0 | 0 | Midterm and final: date, start time and time zone correct |
| MATH 125 | Recurring | 2 | 0 | 2 | n/a | Weekly quiz and weekly homework produced no events |
| MATH 125 | Relative dates | 0 | n/a | n/a | n/a | Syllabus has no "Week N" dates |
| Course B | | | | | | |
| Course C | | | | | | |

## Failure patterns

- **Recurring events are dropped.** The syllabus says quizzes happen every Thursday.
  No dates are listed, so the model returned nothing. The pipeline has no recurrence support.
- **End time is lost.** The source says 10:30–11:30, but the event record has no end
  time field, so the calendar event has only a start.
- **Relative dates:** not observed yet. Need a syllabus with a week-by-week schedule.
- **Table-style schedules:** not observed yet.
- **Hallucinated dates:** none observed in this syllabus. The weight field was
  correctly left empty because the syllabus uses a "best of two weightings" rule.
- **Duplicates:** not observed yet.

## What I changed because of this

- Nothing yet.

## Open problems

- Add end time and location fields to the extracted event.
- Support recurring events (generate from the term range, or use RRULE).
- Test syllabi with relative dates before deciding whether RelativeDateResolver
  should own the date calculation.