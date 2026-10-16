package com.promptly.backend.ai;

import java.security.Principal;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.promptly.backend.ai.dto.SummaryResponse;

/**
 * Endpoints IA (resume PDF via Ollama).
 * Authentification requise : meme regle de visibilite que les cours.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiSummaryService aiSummaryService;

    public AiController(AiSummaryService aiSummaryService) {
        this.aiSummaryService = aiSummaryService;
    }

    @PostMapping("/courses/{courseId}/summarize-pdf")
    public SummaryResponse summarizeCoursePdf(Principal principal,
                                              @PathVariable Long courseId) {
        return aiSummaryService.summarizeCoursePdf(courseId, principal.getName());
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/summarize-pdf")
    public SummaryResponse summarizeChapterPdf(Principal principal,
                                               @PathVariable Long courseId,
                                               @PathVariable Long chapterId) {
        return aiSummaryService.summarizeChapterPdf(courseId, chapterId, principal.getName());
    }
}
