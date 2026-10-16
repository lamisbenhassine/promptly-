package com.promptly.backend.quiz;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.promptly.backend.user.User;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * Ne charge pas questions+choices en meme temps (MultipleBagFetchException).
     * Les choix sont charges en lazy dans le service transactionnel.
     */
    @EntityGraph(attributePaths = {"course", "course.instructor", "chapter", "questions"})
    @Query("""
            SELECT q FROM Quiz q
            WHERE (:courseId IS NULL OR q.course.id = :courseId)
              AND (
                    :seeAll = true
                    OR (q.published = true AND q.course.published = true)
                    OR q.course.instructor = :viewer
                  )
            ORDER BY q.updatedAt DESC
            """)
    List<Quiz> search(@Param("courseId") Long courseId,
                      @Param("seeAll") boolean seeAll,
                      @Param("viewer") User viewer);

    @EntityGraph(attributePaths = {"course", "course.instructor", "chapter", "questions"})
    @Query("SELECT q FROM Quiz q WHERE q.id = :id")
    Optional<Quiz> findWithDetailsById(@Param("id") Long id);
}
