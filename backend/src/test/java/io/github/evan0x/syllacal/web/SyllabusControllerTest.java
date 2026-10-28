package io.github.evan0x.syllacal.web;

import io.github.evan0x.syllacal.extraction.EventExtractor;
import io.github.evan0x.syllacal.extraction.ExtractedEvent;
import io.github.evan0x.syllacal.extraction.ExtractionException;
import io.github.evan0x.syllacal.pdf.PdfTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SyllabusController.class)
@Import({PdfTextExtractor.class, SyllabusControllerTest.FakeExtractor.class})
class SyllabusControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsCalendarForARealPdfEvenWhenTheNameIsNotPdf() throws Exception {
		byte[] pdf = pdf("a".repeat(220));
		MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", pdf);

		mockMvc.perform(multipart("/api/syllabus")
						.file(file)
						.param("courseName", "MATH 101")
						.param("termStart", "2026-08-24")
						.param("termEnd", "2026-12-16"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/calendar")))
				.andExpect(header().string("Content-Disposition", containsString("attachment")))
				.andExpect(header().string("Content-Disposition", containsString("MATH_101.ics")))
				.andExpect(content().string(containsString("BEGIN:VCALENDAR")))
				.andExpect(content().string(containsString("SUMMARY:Exam 1")))
				.andExpect(content().string(containsString("DTSTART;VALUE=DATE:20260831")));
	}

	@Test
	void rejectsNonPdfBytesEvenWithPdfExtension() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "syllabus.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/syllabus")
						.file(file)
						.param("courseName", "MATH 101")
						.param("termStart", "2026-08-24")
						.param("termEnd", "2026-12-16"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("File is not a PDF"));
	}

	@Test
	void rejectsOversizedFile() throws Exception {
		byte[] tooBig = new byte[(int) SyllabusController.MAX_FILE_BYTES + 1];
		System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, tooBig, 0, 5);
		MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", tooBig);

		mockMvc.perform(multipart("/api/syllabus")
						.file(file)
						.param("courseName", "MATH 101")
						.param("termStart", "2026-08-24")
						.param("termEnd", "2026-12-16"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("File exceeds the 10 MB limit"));
	}

	@Test
	void rejectsScannedPdf() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "scan.pdf", "application/pdf", pdf("too short"));

		mockMvc.perform(multipart("/api/syllabus")
						.file(file)
						.param("courseName", "MATH 101")
						.param("termStart", "2026-08-24")
						.param("termEnd", "2026-12-16"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.message", containsString("scanned PDF")));
	}

	@Test
	void rejectsExtractionFailure() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "syllabus.pdf", "application/pdf", pdf("FAIL_EXTRACTION " + "b".repeat(200)));

		mockMvc.perform(multipart("/api/syllabus")
						.file(file)
						.param("courseName", "MATH 101")
						.param("termStart", "2026-08-24")
						.param("termEnd", "2026-12-16"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.message").value("Could not extract events from the syllabus"));
	}

	private static byte[] pdf(String text) throws IOException {
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				content.newLineAtOffset(50, 700);
				content.showText(text);
				content.endText();
			}
			document.save(out);
			return out.toByteArray();
		}
	}

	@TestConfiguration
	static class FakeExtractor {

		@Bean
		EventExtractor eventExtractor() {
			return (syllabusText, termStart, termEnd) -> {
				if (syllabusText.contains("FAIL_EXTRACTION")) {
					throw new ExtractionException("Could not extract events from the syllabus");
				}
				return List.of(new ExtractedEvent(
						"Exam 1",
						"EXAM",
						termStart.plusDays(7),
						null,
						20.0,
						"HIGH",
						"Exam 1"));
			};
		}
	}
}
