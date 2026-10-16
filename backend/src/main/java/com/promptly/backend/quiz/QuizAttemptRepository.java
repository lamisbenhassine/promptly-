package com.promptly.backend.quiz;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuizIdAndStudentIdOrderBySubmittedAtDesc(Long quizId, Long studentId);

    List<QuizAttempt> findByQuizIdOrderBySubmittedAtDesc(Long quizId);
}
