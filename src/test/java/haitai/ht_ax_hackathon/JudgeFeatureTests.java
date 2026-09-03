package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.domain.ApplicationCategory;
import haitai.ht_ax_hackathon.domain.ApplicationStatus;
import haitai.ht_ax_hackathon.domain.EvaluationGuideOverview;
import haitai.ht_ax_hackathon.domain.EvaluationGuideItem;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.Judge;
import haitai.ht_ax_hackathon.domain.JudgeEvaluationStatus;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.domain.TaskSubmissionFile;
import haitai.ht_ax_hackathon.repository.EvaluationGuideItemRepository;
import haitai.ht_ax_hackathon.repository.EvaluationGuideOverviewRepository;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import haitai.ht_ax_hackathon.repository.JudgeEvaluationRepository;
import haitai.ht_ax_hackathon.repository.JudgeRepository;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import haitai.ht_ax_hackathon.service.JudgeEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JudgeFeatureTests {

    @TempDir
    Path tempDirectory;

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
    private Long htmlPreviewFileId;

    @BeforeEach
    void setUp() throws IOException {
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

        HackathonApplication numbered = saveApplication("총무팀2", "AI 해태 챗봇");
        TaskSubmission numberedSubmission = new TaskSubmission(numbered, "번호 대상 제출물", null);
        Path htmlPath = tempDirectory.resolve("demo-package.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(htmlPath), StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("result/demo-page.html"));
            zip.write("<html><body><h1>브라우저 시연</h1></body></html>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        numberedSubmission.addFile(new TaskSubmissionFile(
                "demo-package.zip",
                "demo-package.zip",
                htmlPath.toString(),
                Files.size(htmlPath),
                "application/zip"
        ));
        numberedSubmission = submissionRepository.save(numberedSubmission);
        htmlPreviewFileId = numberedSubmission.getFiles().get(0).getId();
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
                .andExpect(content().string(containsString("AI 해태 챗봇")))
                .andExpect(content().string(not(containsString("총무팀2"))))
                .andExpect(content().string(not(containsString("번호가 없는 과제"))))
                // 목록은 한 줄로 압축되어 점수를 짧은 라벨과 숫자만으로 보여줍니다.
                .andExpect(content().string(containsString("실무자 의견 점수")))
                .andExpect(content().string(containsString(">80<")));

        mockMvc.perform(get("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/evaluations?applicationId=" + numberedApplicationId));

        mockMvc.perform(get("/judge/evaluations/{id}/panel", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE")))
                .andExpect(status().isOk())
                .andExpect(view().name("judge/evaluation-detail :: evaluationPanel"))
                .andExpect(content().string(containsString("AI 해태 챗봇")))
                .andExpect(content().string(containsString("가이드 요약")))
                .andExpect(content().string(containsString("실무자 검토의견")))
                // 과제 내용과 평가는 한 화면으로 합쳤으므로 탭 마크업은 더 이상 없습니다.
                .andExpect(content().string(not(containsString("data-judge-tab"))))
                .andExpect(content().string(containsString("<dt>한 줄 요약</dt>")))
                // 임시 저장과 잠금을 없앴으므로 저장 버튼 하나만 있습니다.
                .andExpect(content().string(not(containsString("data-evaluation-action"))))
                .andExpect(content().string(not(containsString("임시 저장"))))
                .andExpect(content().string(containsString("judge-review-layout")))
                .andExpect(content().string(containsString("judge-score-list")))
                .andExpect(content().string(containsString("judge-practitioner-list")))
                .andExpect(content().string(containsString("시연 안내")))
                .andExpect(content().string(containsString(">admin<")))
                .andExpect(content().string(containsString(">1234<")))
                .andExpect(content().string(containsString(">demo-page.html<")))
                .andExpect(content().string(not(containsString("원본 파일 받기"))))
                .andExpect(content().string(containsString("demo-package.zip")))
                .andExpect(content().string(containsString(" / 20점")))
                .andExpect(content().string(not(containsString("총무팀2"))))
                .andExpect(content().string(not(containsString("<dt>분류</dt>"))));

        mockMvc.perform(get(
                        "/judge/evaluations/{applicationId}/files/{fileId}/preview/result/demo-page.html",
                        numberedApplicationId,
                        htmlPreviewFileId
                ).with(user("judge-test").roles("JUDGE")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Content-Security-Policy", containsString("sandbox")))
                .andExpect(content().string(containsString("브라우저 시연")));

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
    void partialScoresStayEditableAndCompleteOnesCountTowardTheRanking() throws Exception {
        // 일부만 입력하면 작성 중으로 남고 순위에는 들어가지 않습니다.
        mockMvc.perform(post("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE"))
                        .with(csrf())
                        .param("managementEffectScore", "24")
                        .param("fieldUsabilityScore", "25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/judge/evaluations?applicationId=" + numberedApplicationId + "&saved"));

        var draft = evaluationRepository.findAll().get(0);
        assertThat(draft.getStatus()).isEqualTo(JudgeEvaluationStatus.DRAFT);
        assertThat(judgeEvaluationService.findEvaluationTargets("judge-test").get(0).totalScore())
                .isNull();

        // 네 항목을 모두 채우면 평가 완료가 됩니다.
        mockMvc.perform(post("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE"))
                        .with(csrf())
                        .param("managementEffectScore", "25")
                        .param("fieldUsabilityScore", "26")
                        .param("expandabilityScore", "17")
                        .param("innovationScore", "18"))
                .andExpect(status().is3xxRedirection());

        var completed = evaluationRepository.findAll().get(0);
        assertThat(completed.getStatus()).isEqualTo(JudgeEvaluationStatus.SUBMITTED);
        assertThat(completed.getSubmittedAt()).isNotNull();
        assertThat(judgeEvaluationService.findEvaluationTargets("judge-test").get(0).totalScore())
                .isEqualTo(86);

        // 완료된 평가도 계속 고칠 수 있습니다.
        mockMvc.perform(post("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE"))
                        .with(csrf())
                        .param("managementEffectScore", "20")
                        .param("fieldUsabilityScore", "20")
                        .param("expandabilityScore", "10")
                        .param("innovationScore", "10"))
                .andExpect(status().is3xxRedirection());

        assertThat(evaluationRepository.findAll().get(0).getTotalScore()).isEqualTo(60);
    }

    @Test
    void scoresAboveTheCriterionMaximumAreRejected() throws Exception {
        // 경영효과는 30점 만점인데 40점을 보냅니다.
        mockMvc.perform(post("/judge/evaluations/{id}", numberedApplicationId)
                        .with(user("judge-test").roles("JUDGE"))
                        .with(csrf())
                        .param("managementEffectScore", "40")
                        .param("fieldUsabilityScore", "25")
                        .param("expandabilityScore", "16")
                        .param("innovationScore", "17")
                        .param("action", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("judge/evaluation-detail"))
                .andExpect(content().string(containsString("경영효과는 30점을 초과할 수 없습니다.")));

        assertThat(evaluationRepository.findAll()).isEmpty();
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
