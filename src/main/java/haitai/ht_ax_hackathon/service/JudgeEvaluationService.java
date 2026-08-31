package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.EvaluationGuideItem;
import haitai.ht_ax_hackathon.domain.EvaluationGuideOverview;
import haitai.ht_ax_hackathon.domain.Judge;
import haitai.ht_ax_hackathon.domain.JudgeEvaluation;
import haitai.ht_ax_hackathon.domain.JudgeEvaluationStatus;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.dto.JudgeEvaluationDetail;
import haitai.ht_ax_hackathon.dto.JudgeEvaluationForm;
import haitai.ht_ax_hackathon.dto.JudgeRankingRow;
import haitai.ht_ax_hackathon.dto.JudgeSubmissionListItem;
import haitai.ht_ax_hackathon.exception.ApplicationNotFoundException;
import haitai.ht_ax_hackathon.repository.EvaluationGuideItemRepository;
import haitai.ht_ax_hackathon.repository.EvaluationGuideOverviewRepository;
import haitai.ht_ax_hackathon.repository.JudgeEvaluationRepository;
import haitai.ht_ax_hackathon.repository.JudgeRepository;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JudgeEvaluationService {

    private final JudgeRepository judgeRepository;
    private final JudgeEvaluationRepository evaluationRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final EvaluationGuideOverviewRepository guideOverviewRepository;
    private final EvaluationGuideItemRepository guideItemRepository;

    @Transactional(readOnly = true)
    public Judge findJudge(String username) {
        return judgeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("심사자 계정을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<JudgeSubmissionListItem> findEvaluationTargets(String username) {
        Judge judge = findJudge(username);
        Map<Long, JudgeEvaluation> evaluations = evaluationRepository.findByJudgeId(judge.getId()).stream()
                .collect(Collectors.toMap(
                        evaluation -> evaluation.getApplication().getId(),
                        Function.identity()
                ));
        List<TaskSubmission> submissions = submissionRepository.findJudgeEvaluationTargets();
        List<Long> applicationIds = submissions.stream()
                .map(submission -> submission.getApplication().getId())
                .toList();
        Map<Long, EvaluationGuideOverview> overviews = guideOverviewRepository.findAllById(applicationIds).stream()
                .collect(Collectors.toMap(EvaluationGuideOverview::getApplicationId, Function.identity()));

        return submissions.stream()
                .map(submission -> {
                    Long applicationId = submission.getApplication().getId();
                    return toListItem(
                            submission,
                            evaluations.get(applicationId),
                            overviews.get(applicationId)
                    );
                })
                .toList();
    }

    /**
     * 심사자 본인 점수 기준 순위입니다. 전체 심사 결과가 아니라 이 심사자의 평가만 반영합니다.
     * 점수가 확정된 평가 완료 건에만 순위를 매기고, 동점은 공동 순위로 두어 다음 순위를 건너뜁니다.
     * 순위에 들지 못한 과제는 뒤에 이어 붙여 진행 현황까지 한 화면에서 보이게 합니다.
     */
    public List<JudgeRankingRow> buildRanking(List<JudgeSubmissionListItem> targets) {
        List<JudgeSubmissionListItem> scored = targets.stream()
                .filter(JudgeEvaluationService::isRankable)
                .sorted(Comparator.comparing(JudgeSubmissionListItem::totalScore).reversed()
                        .thenComparing(JudgeSubmissionListItem::topic))
                .toList();

        List<JudgeRankingRow> rows = new ArrayList<>();
        Integer previousScore = null;
        int rank = 0;
        for (int index = 0; index < scored.size(); index++) {
            JudgeSubmissionListItem item = scored.get(index);
            if (!item.totalScore().equals(previousScore)) {
                rank = index + 1;
            }
            previousScore = item.totalScore();
            rows.add(JudgeRankingRow.ranked(rank, item));
        }

        targets.stream()
                .filter(item -> !isRankable(item))
                .map(JudgeRankingRow::unranked)
                .forEach(rows::add);
        return rows;
    }

    private static boolean isRankable(JudgeSubmissionListItem item) {
        return item.evaluationStatus() == JudgeEvaluationStatus.SUBMITTED && item.totalScore() != null;
    }

    @Transactional(readOnly = true)
    public JudgeEvaluationDetail findEvaluationDetail(String username, Long applicationId) {
        Judge judge = findJudge(username);
        requireEvaluationTarget(applicationId);
        TaskSubmission submission = submissionRepository.findDetailByApplicationId(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        Optional<JudgeEvaluation> evaluation = evaluationRepository
                .findByJudgeIdAndApplicationId(judge.getId(), applicationId);
        Optional<EvaluationGuideOverview> overview = guideOverviewRepository.findById(applicationId);
        List<EvaluationGuideItem> items = guideItemRepository
                .findByApplicationIdOrderByDisplayOrderAsc(applicationId);
        return new JudgeEvaluationDetail(submission, evaluation, overview, items);
    }

    @Transactional
    public JudgeEvaluation saveEvaluation(String username, Long applicationId,
                                          JudgeEvaluationForm form) {
        validateScores(form);
        Judge judge = findJudge(username);
        requireEvaluationTarget(applicationId);
        TaskSubmission submission = submissionRepository.findDetailByApplicationId(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        JudgeEvaluation evaluation = evaluationRepository
                .findByJudgeIdAndApplicationId(judge.getId(), applicationId)
                .orElseGet(() -> new JudgeEvaluation(judge, submission.getApplication()));

        evaluation.saveScores(
                form.getManagementEffectScore(),
                form.getFieldUsabilityScore(),
                form.getExpandabilityScore(),
                form.getInnovationScore()
        );
        return evaluationRepository.save(evaluation);
    }

    private JudgeSubmissionListItem toListItem(TaskSubmission submission, JudgeEvaluation evaluation,
                                               EvaluationGuideOverview overview) {
        Integer submittedScore = evaluation != null
                && evaluation.getStatus() == JudgeEvaluationStatus.SUBMITTED
                ? evaluation.getTotalScore()
                : null;
        return new JudgeSubmissionListItem(
                submission.getApplication().getId(),
                submission.getApplication().getTopic(),
                submission.getUpdatedAt(),
                evaluation == null ? null : evaluation.getStatus(),
                submittedScore,
                overview == null ? null : overview.getSourceTotalScore()
        );
    }

    private void validateScores(JudgeEvaluationForm form) {
        requireRange(form.getManagementEffectScore(), 30, "경영효과");
        requireRange(form.getFieldUsabilityScore(), 30, "현업 활용 가능성");
        requireRange(form.getExpandabilityScore(), 20, "타부서 적용 가능성");
        requireRange(form.getInnovationScore(), 20, "혁신·창의성");
    }

    private void requireRange(Integer value, int maximum, String fieldName) {
        if (value != null && (value < 0 || value > maximum)) {
            throw new IllegalArgumentException(fieldName + " 점수가 허용 범위를 벗어났습니다.");
        }
    }

    private void requireEvaluationTarget(Long applicationId) {
        boolean hasCompletedGuide = guideOverviewRepository.findById(applicationId)
                .map(overview -> overview.getSourceTotalScore() != null)
                .orElse(false);
        long scoredItemCount = guideItemRepository
                .countByApplicationIdAndWeightedScoreIsNotNull(applicationId);
        if (!hasCompletedGuide || scoredItemCount != 6) {
            throw new ApplicationNotFoundException(applicationId);
        }
    }
}
