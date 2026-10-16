package com.promptly.backend.ai;

import java.nio.file.Path;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.promptly.backend.ai.dto.SummaryResponse;
import com.promptly.backend.chapter.Chapter;
import com.promptly.backend.chapter.ChapterRepository;
import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.course.Course;
import com.promptly.backend.course.CourseService;
import com.promptly.backend.storage.FileStorageService;

/**
 * Resume automatique des PDF de cours / chapitres via Ollama.
 */
@Service
@Transactional(readOnly = true)
public class AiSummaryService {

    private final CourseService courseService;
    private final ChapterRepository chapterRepository;
    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final OllamaClient ollamaClient;

    public AiSummaryService(CourseService courseService,
                            ChapterRepository chapterRepository,
                            FileStorageService fileStorageService,
                            PdfTextExtractor pdfTextExtractor,
                            OllamaClient ollamaClient) {
        this.courseService = courseService;
        this.chapterRepository = chapterRepository;
        this.fileStorageService = fileStorageService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.ollamaClient = ollamaClient;
    }

    public SummaryResponse summarizeCoursePdf(Long courseId, String viewerEmail) {
        Course course = courseService.requireVisible(courseId, viewerEmail);
        String pdfPath = course.getPdfPath();
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new BusinessRuleException("Ce cours n'a pas de PDF a resumer");
        }
        return summarize(pdfPath, course.getTitle());
    }

    public SummaryResponse summarizeChapterPdf(Long courseId, Long chapterId, String viewerEmail) {
        courseService.requireVisible(courseId, viewerEmail);
        Chapter chapter = chapterRepository.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre", chapterId));

        String pdfPath = chapter.getPdfPath();
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new BusinessRuleException("Ce chapitre n'a pas de PDF a resumer");
        }
        return summarize(pdfPath, chapter.getTitle());
    }

    private SummaryResponse summarize(String relativePdfPath, String title) {
        Path absolute = fileStorageService.resolveExisting(relativePdfPath);
        String text = pdfTextExtractor.extract(absolute);
        boolean truncated = text.contains("[Texte tronque pour le resume automatique]");

        String prompt = """
                Tu es un assistant pedagogique pour la plateforme e-learning Promptly.
                Resume le document suivant en francais, de facon claire et structuree.
                Consigne :
                - 5 a 10 phrases ou puces maximum
                - garde les idees essentielles et termes techniques importants
                - ne invente rien qui n'est pas dans le texte
                - commence directement par le resume, sans introduction

                Titre du document : %s

                Contenu :
                %s
                """.formatted(title, text);

        String summary = ollamaClient.generate(prompt);
        return new SummaryResponse(summary, ollamaClient.modelName(), title, truncated);
    }
}
