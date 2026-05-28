package com.sankalp.expensetracker.expense.ocr;

import java.io.IOException;
import java.io.InputStream;

/**
 * Pluggable OCR provider. Select via app.ocr.provider = none | tesseract.
 * Implementations should be safe to call from a background thread.
 */
public interface OcrProvider {
    /** Returns extracted text, or empty string if extraction fails / provider is disabled. */
    String extractText(InputStream imageStream, String contentType) throws IOException;

    /** True if a real OCR engine is wired; false for the no-op default. */
    boolean enabled();
}
