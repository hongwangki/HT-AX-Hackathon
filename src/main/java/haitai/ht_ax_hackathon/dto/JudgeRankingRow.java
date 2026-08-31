package haitai.ht_ax_hackathon.dto;

import haitai.ht_ax_hackathon.domain.JudgeEvaluationStatus;

/**
 * 심사자 본인이 매긴 점수만으로 계산한 순위 한 줄입니다.
 * 평가를 완료하지 않은 과제는 rank 가 null 이며 순위에서 제외됩니다.
 */
public record JudgeRankingRow(
        Integer rank,
        Long applicationId,
        String topic,
        Integer totalScore,
        JudgeEvaluationStatus evaluationStatus
) {

    public static JudgeRankingRow ranked(int rank, JudgeSubmissionListItem item) {
        return new JudgeRankingRow(
                rank,
                item.applicationId(),
                item.topic(),
                item.totalScore(),
                item.evaluationStatus()
        );
    }

    public static JudgeRankingRow unranked(JudgeSubmissionListItem item) {
        return new JudgeRankingRow(
                null,
                item.applicationId(),
                item.topic(),
                item.totalScore(),
                item.evaluationStatus()
        );
    }

    public boolean isRanked() {
        return rank != null;
    }

    public String getStatusLabel() {
        return evaluationStatus == null ? "평가 전" : evaluationStatus.getLabel();
    }
}
