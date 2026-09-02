package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.Judge;
import haitai.ht_ax_hackathon.domain.JudgeEvaluation;
import haitai.ht_ax_hackathon.domain.JudgeEvaluationStatus;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.dto.AdminFinalResultDetail;
import haitai.ht_ax_hackathon.dto.AdminFinalResultSummary;
import haitai.ht_ax_hackathon.dto.AdminJudgeProgressRow;
import haitai.ht_ax_hackathon.dto.AdminJudgeScoreRow;
import haitai.ht_ax_hackathon.exception.ApplicationNotFoundException;
import haitai.ht_ax_hackathon.repository.JudgeEvaluationRepository;
import haitai.ht_ax_hackathon.repository.JudgeRepository;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFinalResultService {

    /** 열람과 개인 평가는 가능하지만 관리자 최종 집계에는 참여하지 않는 계정입니다. */
    private static final Set<String> FINAL_RESULT_EXCLUDED_JUDGE_USERNAMES = Set.of("1273498");

    private final TaskSubmissionRepository submissionRepository;
    private final JudgeRepository judgeRepository;
    private final JudgeEvaluationRepository evaluationRepository;

    @Transactional(readOnly = true)
    public List<AdminFinalResultSummary> findAllResults() {
        List<TaskSubmission> targets = submissionRepository.findJudgeEvaluationTargets();
        List<Judge> judges = findActiveJudges();
        Map<Long, List<JudgeEvaluation>> evaluationsByApplication = findEvaluations(targets, judges).stream()
                .collect(Collectors.groupingBy(evaluation -> evaluation.getApplication().getId()));

        return targets.stream()
                .map(target -> summarize(
                        target.getApplication(),
                        judges.size(),
                        evaluationsByApplication.getOrDefault(target.getApplication().getId(), List.of())
                ))
                .sorted(Comparator
                        .comparing(AdminFinalResultSummary::confirmed).reversed()
                        .thenComparing(AdminFinalResultSummary::averageScore, Comparator.reverseOrder())
                        .thenComparing(AdminFinalResultSummary::teamName))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminFinalResultDetail findResultDetail(Long applicationId) {
        TaskSubmission target = submissionRepository.findJudgeEvaluationTargets().stream()
                .filter(submission -> submission.getApplication().getId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        List<Judge> judges = findActiveJudges();
        List<JudgeEvaluation> evaluations = findEvaluations(List.of(target), judges);
        Map<Long, JudgeEvaluation> evaluationByJudge = evaluations.stream()
                .collect(Collectors.toMap(evaluation -> evaluation.getJudge().getId(), Function.identity()));
        List<AdminJudgeScoreRow> rows = judges.stream()
                .map(judge -> AdminJudgeScoreRow.from(judge, evaluationByJudge.get(judge.getId())))
                .toList();
        return new AdminFinalResultDetail(
                summarize(target.getApplication(), judges.size(), evaluations),
                rows
        );
    }

    @Transactional(readOnly = true)
    public long countActiveJudges() {
        return findActiveJudges().size();
    }

    @Transactional(readOnly = true)
    public List<AdminJudgeProgressRow> findJudgeProgress() {
        List<TaskSubmission> targets = submissionRepository.findJudgeEvaluationTargets();
        List<Judge> judges = findActiveJudges();
        Map<Long, Long> completedByJudge = findEvaluations(targets, judges).stream()
                .filter(evaluation -> evaluation.getStatus() == JudgeEvaluationStatus.SUBMITTED)
                .collect(Collectors.groupingBy(
                        evaluation -> evaluation.getJudge().getId(),
                        Collectors.counting()
                ));

        return judges.stream()
                .map(judge -> new AdminJudgeProgressRow(
                        judge.getId(),
                        judge.getName(),
                        judge.getUsername(),
                        completedByJudge.getOrDefault(judge.getId(), 0L).intValue(),
                        targets.size()
                ))
                .toList();
    }

    private List<Judge> findActiveJudges() {
        return judgeRepository.findByActiveTrueOrderByNameAscUsernameAsc().stream()
                .filter(judge -> !FINAL_RESULT_EXCLUDED_JUDGE_USERNAMES.contains(judge.getUsername()))
                .sorted(Comparator
                        .comparingInt((Judge judge) -> titleOrder(judge.getName()))
                        .thenComparing(Judge::getName)
                        .thenComparing(Judge::getUsername))
                .toList();
    }

    private int titleOrder(String name) {
        if (name == null) {
            return Integer.MAX_VALUE;
        }
        if (name.contains("이사")) return 0;
        if (name.contains("부장")) return 1;
        if (name.contains("차장")) return 2;
        if (name.contains("과장")) return 3;
        if (name.contains("대리")) return 4;
        if (name.contains("사원")) return 5;
        return 6;
    }

    private List<JudgeEvaluation> findEvaluations(List<TaskSubmission> targets, List<Judge> judges) {
        if (targets.isEmpty() || judges.isEmpty()) {
            return List.of();
        }
        List<Long> applicationIds = targets.stream()
                .map(target -> target.getApplication().getId())
                .toList();
        List<Long> judgeIds = judges.stream().map(Judge::getId).toList();
        return evaluationRepository.findForFinalResults(applicationIds, judgeIds);
    }

    private AdminFinalResultSummary summarize(
            HackathonApplication application,
            int totalJudgeCount,
            List<JudgeEvaluation> evaluations
    ) {
        List<JudgeEvaluation> completed = evaluations.stream()
                .filter(evaluation -> evaluation.getStatus() == JudgeEvaluationStatus.SUBMITTED)
                .toList();
        int totalScore = completed.stream().mapToInt(JudgeEvaluation::getTotalScore).sum();
        BigDecimal averageScore = completed.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalScore)
                        .divide(BigDecimal.valueOf(completed.size()), 1, RoundingMode.HALF_UP);
        boolean confirmed = totalJudgeCount > 0 && completed.size() == totalJudgeCount;
        return new AdminFinalResultSummary(
                application.getId(),
                application.getTeamName(),
                application.getTopic(),
                completed.size(),
                totalJudgeCount,
                totalScore,
                averageScore,
                confirmed
        );
    }
}
