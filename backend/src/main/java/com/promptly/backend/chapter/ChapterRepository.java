package com.promptly.backend.chapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByCourseIdOrderByPositionAsc(Long courseId);

    Optional<Chapter> findByIdAndCourseId(Long id, Long courseId);

    @Query("select coalesce(max(c.position), 0) from Chapter c where c.course.id = :courseId")
    int findMaxPosition(@Param("courseId") Long courseId);

    void deleteByCourseId(Long courseId);
}
