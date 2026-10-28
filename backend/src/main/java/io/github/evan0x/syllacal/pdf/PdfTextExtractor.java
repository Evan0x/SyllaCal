package io.github.evan0x.syllacal.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfTextExtractor {

	private static final int MIN_TEXT_LENGTH = 200;

	public String extract(InputStream pdf) throws ScannedPdfException, IOException {
		byte[] bytes = pdf.readAllBytes();
		try (PDDocument document = Loader.loadPDF(bytes)) {
			if (document.isEncrypted()) {
				throw new IOException("Encrypted PDFs are not supported");
			}

			PDFTextStripper stripper = new PDFTextStripper();
			StringBuilder text = new StringBuilder();
			int pages = document.getNumberOfPages();
			for (int page = 1; page <= pages; page++) {
				stripper.setStartPage(page);
				stripper.setEndPage(page);
				if (page > 1) {
					text.append("\n\n");
				}
				text.append(stripper.getText(document).trim());
			}

			String result = text.toString().trim();
			if (result.length() < MIN_TEXT_LENGTH) {
				throw new ScannedPdfException(
						"Extracted text is too short (" + result.length()
								+ " characters). This is probably a scanned PDF; OCR is not supported yet.");
			}
			return result;
		}
	}
}
