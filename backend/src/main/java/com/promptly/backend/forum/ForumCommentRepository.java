package com.promptly.backend.forum;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<ForumComment> findByPostIdOrderByCreatedAtAsc(Long postId);

    @EntityGraph(attributePaths = {"author", "post", "post.author"})
    @Query("SELECT c FROM ForumComment c WHERE c.id = :id AND c.post.id = :postId")
    Optional<ForumComment> findByIdAndPostId(@Param("id") Long id, @Param("postId") Long postId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);
}
