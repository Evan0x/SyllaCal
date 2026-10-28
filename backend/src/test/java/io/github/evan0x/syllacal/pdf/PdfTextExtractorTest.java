package io.github.evan0x.syllacal.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractorTest {

	private final PdfTextExtractor extractor = new PdfTextExtractor();

	@Test
	void extractReturnsPlainTextFromPdf() throws Exception {
		String content = "a".repeat(220);
		byte[] pdf = createPdf(content);

		String text = extractor.extract(new ByteArrayInputStream(pdf));

		assertTrue(text.contains(content));
	}

	@Test
	void extractThrowsWhenTextIsTooShort() throws Exception {
		byte[] pdf = createPdf("too short");

		assertThrows(ScannedPdfException.class,
				() -> extractor.extract(new ByteArrayInputStream(pdf)));
	}

	private static byte[] createPdf(String text) throws IOException {
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
}
