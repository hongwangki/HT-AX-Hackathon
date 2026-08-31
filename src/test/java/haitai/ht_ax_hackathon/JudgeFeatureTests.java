package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.domain.ApplicationCategory;
import haitai.ht_ax_hackathon.domain.ApplicationStatus;
import haitai.ht_ax_hackathon.domain.EvaluationGuideOverview;
import haitai.ht_ax_hackathon.domain.EvaluationGuideItem;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.Judge;
import haitai.ht_ax_hackathon.domain.JudgeEvaluationStatus;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.repository.EvaluationGuideItemRepository;
import haitai.ht_ax_hackathon.repository.EvaluationGuideOverviewRepository;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import haitai.ht_ax_hackathon.repository.JudgeEvaluationRepository;
import haitai.ht_ax_hackathon.repository.JudgeRepository;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import haitai.ht_ax_hackathon.service.JudgeEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JudgeFeatureTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @Autowired
    private JudgeEvaluationService judgeEvaluationService;

    private Long numberedApplicationId;
    private Long unnumberedApplicationId;

    @BeforeEach
    void setUp() {
        evaluationRepository.deleteAll();
        guideItemRepository.deleteAll();
        guideOverviewRepository.deleteAll();
        submissionRepository.deleteAll();
        applicationRepository.deleteAll();
        judgeRepository.deleteAll();

        judgeRepository.save(new Judge(
                "judge-test",
                passwordEncoder.encode("judge-password"),
                "테스트 심사자"
        ));

        HackathonApplication numbered = saveApplication("번호 대상팀", "번호가 있는 과제");
        submissionRepository.save(new TaskSubmission(numbered, "번호 대상 제출물", null));
        guideOverviewRepository.save(new EvaluationGuideOverview(numbered, "가이드 요약", 80));
        for (int order = 1; order <= 6; order++) {
            guideItemRepository.save(new EvaluationGuideItem(
                    numbered,
                    "GUIDE_" + order,
                    "평가항목 " + order,
                    20,
                    4,
                    order == 6 ? 5 : 15,
                    "평가 기준",
                    "상세 코멘트",
                    order
            ));
        }
        numberedApplicationId = numbered.getId();

        HackathonApplication unnumbered = saveApplication("번호 제외팀", "번호가 없는 과제");
        submissionRepository.save(new TaskSubmission(unnumbered, "제외 대상 제출물", null));
        unnumberedApplicationId = unnumbered.getId();
    }

    @Test
    void databaseJudgeCanLoginAndOnlyNumberedTargetsAreListed() throws Exception {
        mockMvc.perform(post("/judge/login")
                        .param("username", "judge-test")
                        .param("password", "judge-password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/evaluations"));

        mockMvc.perform(get("/judge/evaluations")
                        .with(user("judge-test").roles("JUDGE")))
                .andExpect(status().isOk())
                .andExpect(view().name("judge/evaluation-list"))
                .andExpect(content().string(containsString("번호가 있는 과제")))
                .andExpect(content().string(not(containsString("번호 대상팀"))))
                .andExpect(content().string(not(containsString("번호가 없는 과제"))))
                .andExpect(content().string(containsString("실무자 의견")))
                .andExpect(content().string(containsString("80점")))
                .andExpect(content().string(containsString("미부여")));

        mockMvc.perform(get("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/evaluations?applicationId=" + numberedApplicationId));

        mockMvc.perform(get("/judge/evaluations/{id}/panel", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE")))
                .andExpect(status().isOk())
                .andExpect(view().name("judge/evaluation-detail :: evaluationPanel"))
                .andExpect(content().string(containsString("번호가 있는 과제")))
                .andExpect(content().string(containsString("가이드 요약")))
                .andExpect(content().string(containsString("실무자 검토의견")))
                .andExpect(content().string(containsString("data-judge-tab=\"content\"")))
                .andExpect(content().string(containsString("data-judge-tab=\"evaluation\"")))
                .andExpect(content().string(containsString("data-judge-tab-panel=\"content\"")))
                .andExpect(content().string(containsString("data-judge-tab-panel=\"evaluation\"")))
                .andExpect(content().string(containsString("data-evaluation-action-field")))
                .andExpect(content().string(containsString("data-evaluation-action=\"DRAFT\"")))
                .andExpect(content().string(containsString("judge-review-layout")))
                .andExpect(content().string(containsString("judge-score-list")))
                .andExpect(content().string(containsString("judge-practitioner-list")))
                .andExpect(content().string(not(containsString("번호 대상팀"))))
                .andExpect(content().string(not(containsString("<dt>분류</dt>"))));

        mockMvc.perform(get("/judge/evaluations/{id}", unnumberedApplicationId)
                        .with(user("judge-test").roles("JUDGE")))
                .andExpect(status().isOk())
                .andExpect(view().name("error/404"));
    }

    @Test
    void crossRoleAccessRedirectsToTheCorrectLoginPage() throws Exception {
        mockMvc.perform(get("/admin/applications")
                        .with(user("judge-test").roles("JUDGE")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        mockMvc.perform(get("/judge/evaluations")
                        .with(user("admin-test").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/login"));
    }

    @Test
    void judgeLogoutReturnsToJudgeLoginPage() throws Exception {
        mockMvc.perform(post("/judge/logout")
                        .with(user("judge-test").roles("JUDGE"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/login"));
    }

    @Test
    void judgeCanSaveDraftAndSubmitEvaluation() throws Exception {
        mockMvc.perform(post("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE"))
                        .with(csrf())
                        .param("managementEffectScore", "24")
                        .param("fieldUsabilityScore", "25")
                        .param("expandabilityScore", "16")
                        .param("innovationScore", "17")
                        .param("action", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/evaluations?applicationId=" + numberedApplicationId + "&saved"));

        mockMvc.perform(post("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE"))
                        .with(csrf())
                        .param("managementEffectScore", "25")
                        .param("fieldUsabilityScore", "26")
                        .param("expandabilityScore", "17")
                        .param("innovationScore", "18")
                        .param("action", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/evaluations?applicationId=" + numberedApplicationId + "&saved"));

        var draft = evaluationRepository.findAll().get(0);
        assertThat(draft.getStatus()).isEqualTo(JudgeEvaluationStatus.DRAFT);
        assertThat(draft.getTotalScore()).isEqualTo(86);
        assertThat(judgeEvaluationService.findEvaluationTargets("judge-test").get(0).totalScore())
                .isNull();

        mockMvc.perform(post("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE"))
                        .with(csrf())
                        .param("managementEffectScore", "25")
                        .param("fieldUsabilityScore", "26")
                        .param("expandabilityScore", "17")
                        .param("innovationScore", "18")
                        .param("action", "SUBMITTED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/evaluations?applicationId=" + numberedApplicationId + "&submitted"));

        var submitted = evaluationRepository.findAll().get(0);
        assertThat(submitted.getStatus()).isEqualTo(JudgeEvaluationStatus.SUBMITTED);
        assertThat(submitted.getSubmittedAt()).isNotNull();
        assertThat(judgeEvaluationService.findEvaluationTargets("judge-test").get(0).totalScore())
                .isEqualTo(86);
    }

    private HackathonApplication saveApplication(String teamName, String topic) {
        HackathonApplication application = new HackathonApplication(
                teamName,
                ApplicationCategory.MARKETING,
                topic,
                "과제 내용",
                "010-0000-0000",
                "password"
        );
        application.changeStatus(ApplicationStatus.APPROVED);
        return applicationRepository.save(application);
    }
}
