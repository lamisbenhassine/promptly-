package com.promptly.backend.complaint.emotion;

import com.promptly.backend.complaint.ComplaintEmotion;

public record ComplaintEmotionResult(ComplaintEmotion emotion, double confidence) {
}
