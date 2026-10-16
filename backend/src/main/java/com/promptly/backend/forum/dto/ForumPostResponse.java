package com.promptly.backend.forum.dto;

import java.time.Instant;
import java.util.List;

import com.promptly.backend.forum.ForumCategory;
import com.promptly.backend.forum.ForumPost;

public record ForumPostResponse(
        Long id,
        String title,
        String content,
        ForumCategory category,
        boolean locked,
        String imageUrl,
        AuthorSummary author,
        long commentCount,
        List<ForumCommentResponse> comments,
        ReactionSummary reactions,
        Instant createdAt,
        Instant updatedAt
) {
    public static ForumPostResponse summary(ForumPost post, long commentCount,
                                            ReactionSummary reactions) {
        return new ForumPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.isLocked(),
                toPublicUrl(post.getImagePath()),
                AuthorSummary.from(post.getAuthor()),
                commentCount,
                null,
                reactions,
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    public static ForumPostResponse detail(ForumPost post, List<ForumCommentResponse> comments,
                                           ReactionSummary reactions) {
        return new ForumPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.isLocked(),
                toPublicUrl(post.getImagePath()),
                AuthorSummary.from(post.getAuthor()),
                comments.size(),
                comments,
                reactions,
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private static String toPublicUrl(String relativePath) {
        return relativePath == null || relativePath.isBlank()
                ? null
                : "/api/files/" + relativePath;
    }
}
