package com.promptly.backend.ai;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.promptly.backend.ai.dto.FlashcardsResponse;
import com.promptly.backend.ai.dto.GenerateQuizRequest;
import com.promptly.backend.ai.dto.GeneratedQuizResponse;
import com.promptly.backend.ai.dto.SummaryResponse;

import jakarta.validation.Valid;

/**
 * Endpoints IA (resume, flashcards, generation de quiz via Ollama).
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiSummaryService aiSummaryService;
    private final AiQuizService aiQuizService;

    public AiController(AiSummaryService aiSummaryService, AiQuizService aiQuizService) {
        this.aiSummaryService = aiSummaryService;
        this.aiQuizService = aiQuizService;
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

    @PostMapping("/courses/{courseId}/flashcards")
    public FlashcardsResponse flashcardsCourse(Principal principal,
                                               @PathVariable Long courseId) {
        return aiSummaryService.flashcardsForCoursePdf(courseId, principal.getName());
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/flashcards")
    public FlashcardsResponse flashcardsChapter(Principal principal,
                                                @PathVariable Long courseId,
                                                @PathVariable Long chapterId) {
        return aiSummaryService.flashcardsForChapterPdf(courseId, chapterId, principal.getName());
    }

    /**
     * Genere 10 questions QCM a partir du titre, de la description et du cours.
     * Reserve aux enseignants / admins (meme droit que la creation de quiz).
     */
    @PostMapping("/quizzes/generate")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public GeneratedQuizResponse generateQuiz(Principal principal,
                                              @Valid @RequestBody GenerateQuizRequest request) {
        return aiQuizService.generate(request, principal.getName());
    }
}
