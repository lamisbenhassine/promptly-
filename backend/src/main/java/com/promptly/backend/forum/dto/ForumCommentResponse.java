package com.promptly.backend.forum.dto;

import java.time.Instant;

import com.promptly.backend.forum.ForumComment;

public record ForumCommentResponse(
        Long id,
        Long postId,
        String content,
        AuthorSummary author,
        Instant createdAt,
        Instant updatedAt
) {
    public static ForumCommentResponse from(ForumComment comment) {
        return new ForumCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getContent(),
                AuthorSummary.from(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
