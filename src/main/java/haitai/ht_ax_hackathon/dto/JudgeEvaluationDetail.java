package haitai.ht_ax_hackathon.dto;

import haitai.ht_ax_hackathon.domain.EvaluationGuideItem;
import haitai.ht_ax_hackathon.domain.EvaluationGuideOverview;
import haitai.ht_ax_hackathon.domain.JudgeEvaluation;
import haitai.ht_ax_hackathon.domain.TaskSubmission;

import java.util.List;
import java.util.Optional;

public record JudgeEvaluationDetail(
        TaskSubmission submission,
        Optional<JudgeEvaluation> evaluation,
        Optional<EvaluationGuideOverview> guideOverview,
        List<EvaluationGuideItem> guideItems
) {
}
