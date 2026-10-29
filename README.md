Early-stage demo, under active development.

# SyllaCal

SyllaCal takes a course syllabus PDF and generates an `.ics` calendar file with assignment and exam deadlines.

## Features

- A single page where you upload a PDF, enter a course name, and enter a term start and end date.
- The upload is checked as a PDF by its `%PDF-` header, not by the file name. Files over 10 MB are rejected.
- Text is extracted with Apache PDFBox. Pages are separated by a blank line.
- Encrypted PDFs are rejected. A PDF with fewer than 200 characters of extracted text is rejected as a likely scan.
- The text is sent to the Gemini API. The response is parsed as JSON events with a title, type, date, time, weight, confidence, and a short source quote.
- Events with no date are dropped. Events with the same title and date are merged.
- The browser downloads an `.ics` file. An event with no time is all-day. An event with a time is stored in UTC. The description can include the weight and the source quote.

## How it works

The page posts the file and the form fields to `POST /api/syllabus`. The server then:

1. Extracts text from the PDF.
2. Asks Gemini for structured events. The prompt includes the term start and end, and asks the model to turn phrases such as "Week 5 Friday" into a date. The model is called through the `EventExtractor` interface. `GeminiEventExtractor` is the only implementation. HTTP 429 and 5xx responses are retried.
3. Runs `EventValidator`. That step drops undated events and merges duplicate title-and-date pairs. It does not remove events that fall outside the term, and it does not remove events the model marks as low confidence. Those cases are recorded as warnings, and the warnings are not shown on the page or in the file.
4. Builds an iCalendar document with ical4j and returns it as a download.

## Tech stack

- Java 21
- Spring Boot 4.1.2
- Apache PDFBox 3.0.8
- Gemini `generateContent` API, called with Spring `RestClient` (configured model: `gemini-3.5-flash-lite`)
- ical4j 4.4.0
- One HTML page with plain JavaScript

The running application is the Maven project in `backend/`.

## Run locally

Java 21 is required. From the repository root:

```bash
cd backend
export GEMINI_API_KEY=your-key
./mvnw spring-boot:run
```

Open http://localhost:8080. The key is read from the environment. It is not stored in the repo.

## Known limitations

- There is no checked-in log of results on real syllabi (`docs/findings.md` is not in this repo).
- Scanned syllabi are rejected. There is no OCR.
- The request waits on the Gemini call. A slow response leaves the page on "Extracting dates…".
- Timed events use UTC. The form has no timezone field. Events do not get an end time.
- The prompt assumes the term starts on a Monday. The form does not check that.
- Relative dates are only interpreted by the model. `RelativeDateResolver` is not implemented and is not used by the upload path.
- `Term.overlaps`, `Term.contains`, and `Term.getDurationInDays` are not implemented. The upload path does not use them. `./mvnw test` still runs those tests, and they fail.
- Validator warnings are not returned to the browser.

## Roadmap

- User accounts and saved calendars
- OCR for scanned PDFs
- Asynchronous extraction, so the page does not wait on the model call
- Cloud deployment. `backend/Dockerfile` and `k8s/` are unused sketches
- Reminders
- A finished term model and relative-date resolver
- Show validator warnings, and let the user choose the timezone used for timed events
