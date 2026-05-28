package com.sankalp.expensetracker.expense.ocr;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tesseract-backed OCR. Requires tesseract installed on the host/container:
 *   apt-get install -y tesseract-ocr tesseract-ocr-eng
 * Falls back to empty string on error (never propagates exceptions to the upload flow).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ocr.provider", havingValue = "tesseract")
public class TesseractOcrProvider implements OcrProvider {

    private final Tesseract tesseract;

    public TesseractOcrProvider(
            @Value("${app.ocr.tesseract.data-path:/usr/share/tesseract-ocr/4.00/tessdata}") String dataPath,
            @Value("${app.ocr.tesseract.language:eng}") String language) {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(dataPath);
        this.tesseract.setLanguage(language);
        log.info("Tesseract OCR initialised: dataPath={} language={}", dataPath, language);
    }

    @Override
    public String extractText(InputStream is, String contentType) {
        try {
            var image = ImageIO.read(is);
            if (image == null) {
                log.warn("Could not decode receipt image ({}); skipping OCR", contentType);
                return "";
            }
            return tesseract.doOCR(image);
        } catch (Exception e) {
            log.warn("OCR failed: {}", e.getMessage());
            return "";
        }
    }

    @Override public boolean enabled() { return true; }
}
