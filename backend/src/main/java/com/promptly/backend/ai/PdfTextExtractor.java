package com.promptly.backend.ai;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.promptly.backend.common.exception.BusinessRuleException;

/** Extrait le texte d'un PDF local (Apache PDFBox). */
@Service
public class PdfTextExtractor {

    private final int maxChars;

    public PdfTextExtractor(@Value("${promptly.ai.max-pdf-chars:12000}") int maxChars) {
        this.maxChars = Math.max(1000, maxChars);
    }

    public String extract(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (document.getNumberOfPages() == 0) {
                throw new BusinessRuleException("Le PDF ne contient aucune page");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new BusinessRuleException(
                        "Aucun texte extractible dans ce PDF (scan image non supporte)");
            }

            String normalized = text.replaceAll("\\s+", " ").trim();
            if (normalized.length() > maxChars) {
                return normalized.substring(0, maxChars)
                        + "\n\n[Texte tronque pour le resume automatique]";
            }
            return normalized;
        } catch (IOException e) {
            throw new BusinessRuleException("Impossible de lire le PDF : " + e.getMessage());
        }
    }
}
