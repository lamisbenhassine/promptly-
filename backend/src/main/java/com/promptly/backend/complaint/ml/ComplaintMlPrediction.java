package com.promptly.backend.complaint.ml;

import com.promptly.backend.complaint.ComplaintCategory;
import com.promptly.backend.complaint.ComplaintPriority;
import com.promptly.backend.complaint.ComplaintUrgency;

/** Resultat du classifieur ML (type / urgence / priorite). */
public record ComplaintMlPrediction(
        ComplaintCategory category,
        ComplaintUrgency urgency,
        ComplaintPriority priority,
        double confidence
) {
}
