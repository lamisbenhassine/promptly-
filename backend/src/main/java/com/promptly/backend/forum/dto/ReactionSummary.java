package com.promptly.backend.forum.dto;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.promptly.backend.forum.ReactionType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReactionSummary(
        long total,
        Map<ReactionType, Long> counts,
        ReactionType myReaction
) {
    public static ReactionSummary from(List<Object[]> rows, ReactionType myReaction) {
        Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        long total = 0;
        for (Object[] row : rows) {
            ReactionType type = (ReactionType) row[0];
            long count = (Long) row[1];
            counts.put(type, count);
            total += count;
        }
        return new ReactionSummary(total, counts, myReaction);
    }

    public static ReactionSummary empty() {
        return new ReactionSummary(0, Map.of(), null);
    }
}
