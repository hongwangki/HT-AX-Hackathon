package haitai.ht_ax_hackathon.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 관리자 최종 결과 목록의 팀별 집계 한 줄입니다. */
public record AdminFinalResultSummary(
        Long applicationId,
        String teamName,
        String topic,
        int completedJudgeCount,
        int totalJudgeCount,
        int totalScore,
        BigDecimal averageScore,
        boolean confirmed
) {

    public boolean hasScores() {
        return completedJudgeCount > 0;
    }

    public String getAverageDisplay() {
        return averageScore.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    public String getStatusLabel() {
        if (totalJudgeCount == 0) {
            return "심사관 없음";
        }
        return confirmed ? "확정" : "집계 중";
    }
}
