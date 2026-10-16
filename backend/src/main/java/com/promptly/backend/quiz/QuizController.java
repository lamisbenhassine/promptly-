package com.promptly.backend.quiz;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.promptly.backend.quiz.dto.AttemptRequest;
import com.promptly.backend.quiz.dto.AttemptResponse;
import com.promptly.backend.quiz.dto.QuizRequest;
import com.promptly.backend.quiz.dto.QuizResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public List<QuizResponse> list(Principal principal,
                                   @RequestParam(required = false) Long courseId) {
        return quizService.list(principal.getName(), courseId);
    }

    @GetMapping("/{id}")
    public QuizResponse get(Principal principal, @PathVariable Long id) {
        return quizService.get(id, principal.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<QuizResponse> create(Principal principal,
                                               @Valid @RequestBody QuizRequest request) {
        QuizResponse created = quizService.create(principal.getName(), request);
        return ResponseEntity
                .created(URI.create("/api/quizzes/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public QuizResponse update(Principal principal,
                               @PathVariable Long id,
                               @Valid @RequestBody QuizRequest request) {
        return quizService.update(id, principal.getName(), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long id) {
        quizService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/attempts")
    public AttemptResponse submit(Principal principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody AttemptRequest request) {
        return quizService.submit(id, principal.getName(), request);
    }

    @GetMapping("/{id}/attempts/me")
    public List<AttemptResponse> myAttempts(Principal principal, @PathVariable Long id) {
        return quizService.myAttempts(id, principal.getName());
    }

    @GetMapping("/{id}/attempts")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public List<AttemptResponse> allAttempts(Principal principal, @PathVariable Long id) {
        return quizService.allAttempts(id, principal.getName());
    }
}
