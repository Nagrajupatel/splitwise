package com.splitwise.service;

import com.splitwise.web.Dtos.OcrResult;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads a bill photo with Tesseract and pulls out the merchant and the total. */
@Service
public class OcrService {
    private static final Pattern AMOUNT = Pattern.compile("(\\d{1,3}(?:,\\d{3})+|\\d+)[.,]\\d{2}\\b");

    @Value("${ocr.tessdata-path:./tessdata}")
    private String tessdataPath;

    public OcrResult scan(MultipartFile file) {
        String text = readText(file);
        return new OcrResult(guessMerchant(text), guessTotal(text), text);
    }

    private String readText(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) throw new IllegalArgumentException("That file isn't a readable image. Upload a JPG or PNG.");

            Tesseract tesseract = new Tesseract(); // not thread-safe, so one per request
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("eng");
            return tesseract.doOCR(image);
        } catch (IOException | TesseractException e) {
            throw new IllegalStateException("Couldn't read the bill: " + e.getMessage(), e);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new IllegalStateException("Tesseract isn't installed on the server. See the README for OCR setup.", e);
        }
    }

    /** Prefers a line containing "total" (but not "subtotal"); falls back to the biggest amount on the bill. */
    static BigDecimal guessTotal(String text) {
        BigDecimal labelled = null;
        BigDecimal largest = null;

        for (String line : text.split("\\R")) {
            String lower = line.toLowerCase();
            BigDecimal lastOnLine = null;
            Matcher m = AMOUNT.matcher(line);
            while (m.find()) {
                lastOnLine = parse(m.group());
                if (largest == null || lastOnLine.compareTo(largest) > 0) largest = lastOnLine;
            }
            if (lastOnLine != null && lower.contains("total") && !lower.contains("sub")) {
                labelled = lastOnLine;
            }
        }
        return labelled != null ? labelled : largest;
    }

    static String guessMerchant(String text) {
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) return line.trim();
        }
        return "";
    }

    private static BigDecimal parse(String raw) {
        String clean = raw.replace(",", ".");
        int lastDot = clean.lastIndexOf('.');
        // anything before the last dot was a thousands separator
        String whole = clean.substring(0, lastDot).replace(".", "");
        return new BigDecimal(whole + clean.substring(lastDot));
    }
}
