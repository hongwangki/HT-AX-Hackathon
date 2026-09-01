package haitai.ht_ax_hackathon.dto;

import haitai.ht_ax_hackathon.domain.Judge;
import haitai.ht_ax_hackathon.domain.JudgeEvaluation;
import haitai.ht_ax_hackathon.domain.JudgeEvaluationStatus;

/** 관리자 최종 결과 상세의 심사관별 점수 한 줄입니다. */
public record AdminJudgeScoreRow(
        Long judgeId,
        String judgeName,
        String username,
        Integer managementEffectScore,
        Integer fieldUsabilityScore,
        Integer expandabilityScore,
        Integer innovationScore,
        Integer totalScore,
        JudgeEvaluationStatus status
) {

    public static AdminJudgeScoreRow from(Judge judge, JudgeEvaluation evaluation) {
        if (evaluation == null) {
            return new AdminJudgeScoreRow(
                    judge.getId(), judge.getName(), judge.getUsername(),
                    null, null, null, null, null, null
            );
        }
        boolean hasAnyScore = evaluation.getManagementEffectScore() != null
                || evaluation.getFieldUsabilityScore() != null
                || evaluation.getExpandabilityScore() != null
                || evaluation.getInnovationScore() != null;
        return new AdminJudgeScoreRow(
                judge.getId(),
                judge.getName(),
                judge.getUsername(),
                evaluation.getManagementEffectScore(),
                evaluation.getFieldUsabilityScore(),
                evaluation.getExpandabilityScore(),
                evaluation.getInnovationScore(),
                hasAnyScore ? evaluation.getTotalScore() : null,
                evaluation.getStatus()
        );
    }

    public String getStatusLabel() {
        return status == null ? "평가 전" : status.getLabel();
    }

    public boolean isSubmitted() {
        return status == JudgeEvaluationStatus.SUBMITTED;
    }
}
