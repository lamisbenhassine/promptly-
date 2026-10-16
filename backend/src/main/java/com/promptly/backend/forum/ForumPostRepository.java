package com.promptly.backend.forum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    @EntityGraph(attributePaths = {"author"})
    @Query("""
            SELECT p FROM ForumPost p
            WHERE (:category IS NULL OR p.category = :category)
            ORDER BY p.updatedAt DESC
            """)
    Page<ForumPost> search(@Param("category") ForumCategory category, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT p FROM ForumPost p WHERE p.id = :id")
    java.util.Optional<ForumPost> findWithAuthorById(@Param("id") Long id);
}
