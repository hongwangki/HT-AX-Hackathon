package haitai.ht_ax_hackathon.dto;

import java.util.List;

/** 팀 집계와 전체 활성 심사관의 점수 내역을 함께 전달합니다. */
public record AdminFinalResultDetail(
        AdminFinalResultSummary summary,
        List<AdminJudgeScoreRow> judgeScores
) {
}
