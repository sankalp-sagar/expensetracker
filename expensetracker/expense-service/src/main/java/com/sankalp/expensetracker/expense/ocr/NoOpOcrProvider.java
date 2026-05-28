package com.sankalp.expensetracker.expense.ocr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Default OCR provider — does nothing. Active unless app.ocr.provider explicitly = tesseract / vision.
 */
@Component
@ConditionalOnProperty(name = "app.ocr.provider", havingValue = "none", matchIfMissing = true)
public class NoOpOcrProvider implements OcrProvider {
    @Override public String extractText(InputStream is, String contentType) { return ""; }
    @Override public boolean enabled() { return false; }
}
