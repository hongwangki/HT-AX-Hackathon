package haitai.ht_ax_hackathon.dto;

/** 관리자용 심사관별 평가 진행 현황입니다. */
public record AdminJudgeProgressRow(
        Long judgeId,
        String judgeName,
        String username,
        int completedCount,
        int totalCount
) {

    public int remainingCount() {
        return Math.max(0, totalCount - completedCount);
    }

    public double progressRate() {
        return totalCount == 0 ? 0 : (double) completedCount / totalCount;
    }

    public String statusLabel() {
        if (totalCount == 0) return "대상 없음";
        if (completedCount == totalCount) return "완료";
        if (completedCount == 0) return "미시작";
        return "진행 중";
    }
}
