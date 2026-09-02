package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.domain.ApplicationCategory;
import haitai.ht_ax_hackathon.domain.ApplicationStatus;
import haitai.ht_ax_hackathon.domain.EvaluationGuideItem;
import haitai.ht_ax_hackathon.domain.EvaluationGuideOverview;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.Judge;
import haitai.ht_ax_hackathon.domain.JudgeEvaluation;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.repository.EvaluationGuideItemRepository;
import haitai.ht_ax_hackathon.repository.EvaluationGuideOverviewRepository;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import haitai.ht_ax_hackathon.repository.JudgeEvaluationRepository;
import haitai.ht_ax_hackathon.repository.JudgeRepository;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import haitai.ht_ax_hackathon.service.AdminFinalResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminFinalResultFeatureTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminFinalResultService finalResultService;

    @Autowired
    private JudgeRepository judgeRepository;

    @Autowired
    private JudgeEvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationGuideOverviewRepository guideOverviewRepository;

    @Autowired
    private EvaluationGuideItemRepository guideItemRepository;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private HackathonApplicationRepository applicationRepository;

    private Judge firstJudge;
    private Judge secondJudge;
    private Judge thirdJudge;
    private Judge excludedViewer;
    private JudgeEvaluation thirdDraft;
    private Long applicationId;

    @BeforeEach
    void setUp() {
        evaluationRepository.deleteAll();
        guideItemRepository.deleteAll();
        guideOverviewRepository.deleteAll();
        submissionRepository.deleteAll();
        applicationRepository.deleteAll();
        judgeRepository.deleteAll();

        firstJudge = judgeRepository.save(new Judge("0000001", "hash", "김심사 부장"));
        secondJudge = judgeRepository.save(new Judge("0000002", "hash", "박심사 이사"));
        thirdJudge = judgeRepository.save(new Judge("0000003", "hash", "이심사 과장"));
        excludedViewer = judgeRepository.save(new Judge("1273498", "hash", "열람 심사자"));

        HackathonApplication application = new HackathonApplication(
                "집계 테스트팀",
                ApplicationCategory.MARKETING,
                "팀별 최종 결과 테스트 과제",
                "과제 내용",
                "010-1111-2222",
                "password"
        );
        application.changeStatus(ApplicationStatus.APPROVED);
        application = applicationRepository.save(application);
        applicationId = application.getId();
        submissionRepository.save(new TaskSubmission(application, "최종 제출", null));
        guideOverviewRepository.save(new EvaluationGuideOverview(application, "한 줄 요약", 80));
        for (int order = 1; order <= 6; order++) {
            guideItemRepository.save(new EvaluationGuideItem(
                    application,
                    "FINAL_" + order,
                    "평가항목 " + order,
                    20,
                    4,
                    order == 6 ? 5 : 15,
                    "평가 기준",
                    "상세 코멘트",
                    order
            ));
        }

        saveCompletedEvaluation(firstJudge, application, 20, 25, 17, 18); // 80점
        saveCompletedEvaluation(secondJudge, application, 28, 27, 18, 17); // 90점
        saveCompletedEvaluation(excludedViewer, application, 30, 30, 20, 20); // 집계 제외 100점
        thirdDraft = new JudgeEvaluation(thirdJudge, application);
        thirdDraft.saveScores(10, null, null, null);
        thirdDraft = evaluationRepository.save(thirdDraft);
        evaluationRepository.flush();
    }

    @Test
    void incompleteTeamShowsProvisionalTotalsAndBecomesConfirmedWhenEveryoneFinishes() throws Exception {
        var provisional = finalResultService.findAllResults().get(0);
        assertThat(provisional.completedJudgeCount()).isEqualTo(2);
        assertThat(provisional.totalJudgeCount()).isEqualTo(3);
        assertThat(provisional.totalScore()).isEqualTo(170);
        assertThat(provisional.getAverageDisplay()).isEqualTo("85.0");
        assertThat(provisional.confirmed()).isFalse();
        assertThat(provisional.getStatusLabel()).isEqualTo("집계 중");

        var detail = finalResultService.findResultDetail(applicationId);
        assertThat(detail.judgeScores()).hasSize(3);
        assertThat(finalResultService.countActiveJudges()).isEqualTo(3);
        assertThat(detail.judgeScores())
                .extracting(row -> row.judgeName())
                .containsExactly("박심사 이사", "김심사 부장", "이심사 과장");
        assertThat(detail.judgeScores())
                .extracting(row -> row.username())
                .doesNotContain("1273498");
        assertThat(detail.judgeScores())
                .filteredOn(row -> row.username().equals("0000003"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.managementEffectScore()).isEqualTo(10);
                    assertThat(row.totalScore()).isEqualTo(10);
                    assertThat(row.getStatusLabel()).isEqualTo("작성 중");
                });

        mockMvc.perform(get("/admin/final-results"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/final-result-list"))
                .andExpect(content().string(containsString("집계 테스트팀")))
                .andExpect(content().string(containsString("170점")))
                .andExpect(content().string(containsString("85.0점")))
                .andExpect(content().string(containsString("집계 중")));

        mockMvc.perform(get("/admin/final-results/{id}", applicationId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/final-result-detail"))
                .andExpect(content().string(containsString("김심사 부장")))
                .andExpect(content().string(containsString("박심사 이사")))
                .andExpect(content().string(containsString("이심사 과장")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("1273498"))))
                .andExpect(content().string(containsString("작성 중")));

        thirdDraft.saveScores(20, 20, 15, 15); // 70점
        evaluationRepository.saveAndFlush(thirdDraft);

        var confirmed = finalResultService.findAllResults().get(0);
        assertThat(confirmed.completedJudgeCount()).isEqualTo(3);
        assertThat(confirmed.totalScore()).isEqualTo(240);
        assertThat(confirmed.getAverageDisplay()).isEqualTo("80.0");
        assertThat(confirmed.confirmed()).isTrue();
        assertThat(confirmed.getStatusLabel()).isEqualTo("확정");
    }

    private void saveCompletedEvaluation(
            Judge judge,
            HackathonApplication application,
            int management,
            int usability,
            int expandability,
            int innovation
    ) {
        JudgeEvaluation evaluation = new JudgeEvaluation(judge, application);
        evaluation.saveScores(management, usability, expandability, innovation);
        evaluationRepository.save(evaluation);
    }
}
