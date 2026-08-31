package haitai.ht_ax_hackathon.dto;

import haitai.ht_ax_hackathon.domain.JudgeEvaluationStatus;

import java.time.LocalDateTime;

public record JudgeSubmissionListItem(
        Long applicationId,
        String topic,
        LocalDateTime submittedAt,
        JudgeEvaluationStatus evaluationStatus,
        Integer totalScore,
        Integer practitionerTotalScore
) {

    public String getStatusLabel() {
        return evaluationStatus == null ? "평가 전" : evaluationStatus.getLabel();
    }
}
