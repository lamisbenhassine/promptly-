package com.promptly.backend.forum;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForumReactionRepository extends JpaRepository<ForumReaction, Long> {

    Optional<ForumReaction> findByPostIdAndUserId(Long postId, Long userId);

    List<ForumReaction> findByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("""
            SELECT r.type, COUNT(r)
            FROM ForumReaction r
            WHERE r.post.id = :postId
            GROUP BY r.type
            """)
    List<Object[]> countByTypeForPost(@Param("postId") Long postId);
}
