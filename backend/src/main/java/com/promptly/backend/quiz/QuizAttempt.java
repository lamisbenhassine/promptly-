package com.promptly.backend.quiz;

import java.time.Instant;

import com.promptly.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Tentative de passage d'un quiz.
 *
 * Les reponses sont stockees sous forme de JSON (liste d'identifiants de choix)
 * pour rester simple et auditable sans table intermediaire.
 */
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private int scorePercent;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int totalQuestions;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String selectedChoiceIdsJson;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    protected QuizAttempt() {
    }

    public QuizAttempt(Quiz quiz, User student, int scorePercent, int correctCount,
                       int totalQuestions, String selectedChoiceIdsJson) {
        this.quiz = quiz;
        this.student = student;
        this.scorePercent = scorePercent;
        this.correctCount = correctCount;
        this.totalQuestions = totalQuestions;
        this.selectedChoiceIdsJson = selectedChoiceIdsJson;
    }

    @PrePersist
    void onCreate() {
        this.submittedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public User getStudent() {
        return student;
    }

    public int getScorePercent() {
        return scorePercent;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public String getSelectedChoiceIdsJson() {
        return selectedChoiceIdsJson;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
