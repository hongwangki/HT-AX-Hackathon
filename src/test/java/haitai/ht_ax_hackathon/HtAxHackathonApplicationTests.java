package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.domain.ApplicationStatus;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import haitai.ht_ax_hackathon.service.HackathonApplicationService;
import haitai.ht_ax_hackathon.service.TaskSubmissionService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@SpringBootTest
@WithMockUser(roles = "ADMIN")
class HtAxHackathonApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HackathonApplicationRepository applicationRepository;

    @Autowired
    private HackathonApplicationService applicationService;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private TaskSubmissionService submissionService;

    @Test
    void mainPagesAndApplicationLifecycleWork() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        mockMvc.perform(get("/apply"))
                .andExpect(status().isOk())
                .andExpect(view().name("apply/form"));

        MockMultipartFile attachment = new MockMultipartFile(
                "attachments", "proposal.txt", "text/plain", "proposal".getBytes()
        );

        mockMvc.perform(multipart("/apply")
                        .file(attachment)
                        .param("teamName", "AX Test Team")
                        .param("representativePhone", "010-1234-5678")
                        .param("password", "test1234")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "1000001")
                        .param("members[0].name", "Tester")
                        .param("category", "MARKETING")
                        .param("topic", "Internal AI Assistant")
                        .param("content", "Test application content")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/apply/complete"));

        Long id = applicationRepository.findAll().get(0).getId();
        var savedApplication = applicationService.findApplication(id);
        Long fileId = savedApplication.getFiles().get(0).getId();
        Path uploadedFile = Path.of(savedApplication.getFiles().get(0).getFilePath());
        assertThat(Files.exists(uploadedFile)).isTrue();

        mockMvc.perform(get("/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/application-list"));

        byte[] excelContent = mockMvc.perform(get("/admin/applications/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(excelContent))) {
            assertThat(workbook.getSheet("팀별 신청 요약")).isNotNull();
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(3).getCell(0).getStringCellValue())
                    .isEqualTo("1팀  |  AX Test Team");
            assertThat(workbook.getSheet("팀별 신청 요약").getNumMergedRegions()).isGreaterThan(1);
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(4).getCell(0).getStringCellValue())
                    .isEqualTo("구성인원");
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(6).getCell(0).getStringCellValue())
                    .isEqualTo("분류");
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(6).getCell(1).getStringCellValue())
                    .isEqualTo("마케팅");
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(7).getCell(0).getStringCellValue())
                    .isEqualTo("주제");
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(8).getCell(0).getStringCellValue())
                    .isEqualTo("아이디어 내용");
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(8).getHeightInPoints()).isGreaterThanOrEqualTo(42);
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(10).getCell(0).getStringCellValue())
                    .isEqualTo("심사 상태");
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(10).getCell(1).getStringCellValue())
                    .isEqualTo("접수");
            assertThat(workbook.getSheet("팀별 신청 요약").getColumnWidth(1)).isEqualTo(9 * 256);
            assertThat(workbook.getSheet("팀별 신청 요약").getRow(5).getCell(0).getCellStyle())
                    .isEqualTo(workbook.getSheet("팀별 신청 요약").getRow(4).getCell(0).getCellStyle());
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            // Not approved yet, so the approved-only sheet shows the empty notice.
            assertThat(workbook.getSheet("승인된 팀")).isNotNull();
            assertThat(workbook.getSheet("승인된 팀").getRow(3).getCell(0).getStringCellValue())
                    .isEqualTo("표시할 팀이 없습니다.");
        }

        mockMvc.perform(get("/admin/applications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/application-detail"));

        mockMvc.perform(post("/admin/applications/{id}/approve", id).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/applications/" + id));
        assertThat(applicationService.findApplication(id).getStatus()).isEqualTo(ApplicationStatus.APPROVED);

        mockMvc.perform(get("/admin/applications").param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/application-list"))
                .andExpect(model().attribute("selectedStatus", ApplicationStatus.APPROVED))
                .andExpect(model().attribute("applications", hasSize(1)))
                .andExpect(model().attribute("filteredApplicationCount", 1));

        byte[] approvedExcel = mockMvc.perform(get("/admin/applications/export"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(approvedExcel))) {
            var statusCell = workbook.getSheet("팀별 신청 요약").getRow(10).getCell(1);
            assertThat(statusCell.getStringCellValue()).isEqualTo("승인");
            assertThat(((org.apache.poi.xssf.usermodel.XSSFCellStyle) statusCell.getCellStyle())
                    .getFillForegroundColorColor().getRGB())
                    .containsExactly((byte) 222, (byte) 247, (byte) 235);
            // The approved team now appears on the approved-only sheet in the same block format.
            assertThat(workbook.getSheet("승인된 팀").getRow(3).getCell(0).getStringCellValue())
                    .isEqualTo("1팀  |  AX Test Team");
        }

        mockMvc.perform(post("/admin/applications/{id}/reject", id).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/applications/" + id));
        assertThat(applicationService.findApplication(id).getStatus()).isEqualTo(ApplicationStatus.REJECTED);

        mockMvc.perform(get("/admin/applications").param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("applications", hasSize(0)))
                .andExpect(model().attribute("filteredApplicationCount", 0));

        mockMvc.perform(get("/admin/applications").param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStatus", ApplicationStatus.REJECTED))
                .andExpect(model().attribute("applications", hasSize(1)))
                .andExpect(model().attribute("filteredApplicationCount", 1));

        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("status/check"));

        mockMvc.perform(post("/status").with(csrf())
                        .param("representativePhone", "010-1234-5678")
                        .param("password", "test1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/status/result"));

        mockMvc.perform(post("/status").with(csrf())
                        .param("representativePhone", "010-1234-5678")
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("status/check"));

        mockMvc.perform(get("/admin/applications/{id}/files/{fileId}/download", id, fileId))
                .andExpect(status().isOk())
                .andExpect(content().bytes("proposal".getBytes()));

        mockMvc.perform(get("/admin/applications/{id}/edit", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/application-edit"));

        mockMvc.perform(post("/admin/applications/{id}/files/{fileId}/delete", id, fileId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/applications/" + id + "/edit"));

        assertThat(applicationService.findApplication(id).getFiles()).isEmpty();
        assertThat(Files.exists(uploadedFile)).isFalse();

        mockMvc.perform(multipart("/admin/applications/{id}/edit", id)
                        .param("teamName", "Updated AX Team")
                        .param("representativePhone", "010-9876-5432")
                        .param("members[0].department", "Updated Team")
                        .param("members[0].employeeNo", "2000002")
                        .param("members[0].name", "Updated Tester")
                        .param("category", "MARKETING")
                        .param("topic", "Updated Topic")
                        .param("content", "Updated content")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/applications/" + id));

        assertThat(applicationService.findApplication(id).getTeamName()).isEqualTo("Updated AX Team");

        mockMvc.perform(get("/admin/applications/{id}/delete", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/application-delete"));

        mockMvc.perform(post("/admin/applications/{id}/delete", id).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/applications"));

        assertThat(applicationRepository.count()).isZero();
        assertThat(Files.exists(uploadedFile)).isFalse();
    }

    @Test
    void reapplicationMustReuseTheSamePassword() throws Exception {
        mockMvc.perform(multipart("/apply")
                        .param("teamName", "Retry Team")
                        .param("representativePhone", "010-5555-6666")
                        .param("password", "first123")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "3000003")
                        .param("members[0].name", "Tester")
                        .param("category", "MARKETING")
                        .param("topic", "First Topic")
                        .param("content", "First content")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/apply/complete"));

        // Re-applying with a different password is rejected so one credential pair unlocks everything.
        mockMvc.perform(multipart("/apply")
                        .param("teamName", "Retry Team")
                        .param("representativePhone", "010-5555-6666")
                        .param("password", "different999")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "3000003")
                        .param("members[0].name", "Tester")
                        .param("category", "MARKETING")
                        .param("topic", "Second Topic")
                        .param("content", "Second content")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("apply/form"));

        mockMvc.perform(multipart("/apply")
                        .param("teamName", "Retry Team")
                        .param("representativePhone", "010-5555-6666")
                        .param("password", "first123")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "3000003")
                        .param("members[0].name", "Tester")
                        .param("category", "MARKETING")
                        .param("topic", "Second Topic")
                        .param("content", "Second content")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/apply/complete"));

        // One phone + password pair returns the full submission history.
        assertThat(applicationService.findMyApplications("010-5555-6666", "first123")).hasSize(2);
        assertThat(applicationService.findMyApplications("010-5555-6666", "different999")).isEmpty();

        // Admin looks the stored password up by phone number to relay it to the applicant.
        mockMvc.perform(post("/admin/applications/password-lookup").with(csrf())
                        .param("representativePhone", "010-5555-6666"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/password-lookup"))
                .andExpect(model().attribute("foundPassword", "first123"));
        assertThat(applicationService.findPasswordByPhone("010-5555-6666")).contains("first123");

        // Looking up an unknown phone number stays on the form with an error.
        mockMvc.perform(post("/admin/applications/password-lookup").with(csrf())
                        .param("representativePhone", "010-0000-0000"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/password-lookup"))
                .andExpect(model().attributeDoesNotExist("foundPassword"));

        applicationRepository.deleteAll();
    }

    @Test
    void taskSubmissionFlowWorks() throws Exception {
        mockMvc.perform(multipart("/apply")
                        .param("teamName", "Submission Team")
                        .param("representativePhone", "010-7777-8888")
                        .param("password", "submit123")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "4000004")
                        .param("members[0].name", "홍길동")
                        .param("members[1].department", "AX Team")
                        .param("members[1].employeeNo", "4000005")
                        .param("members[1].name", "홍길길")
                        .param("category", "MARKETING")
                        .param("topic", "Submission Topic")
                        .param("content", "Submission content")
                        .with(csrf()))
                .andExpect(redirectedUrl("/apply/complete"));
        Long id = applicationRepository.findAll().get(0).getId();

        // Not approved yet: the submission entry point bounces back to the status page.
        mockMvc.perform(post("/submit").with(csrf())
                        .param("applicationId", id.toString())
                        .param("representativePhone", "010-7777-8888")
                        .param("password", "submit123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/status"));

        mockMvc.perform(post("/admin/applications/{id}/approve", id).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Opening the result check cascades to the submission window.
        mockMvc.perform(post("/admin/applications/status-access").with(csrf()).param("open", "true"))
                .andExpect(redirectedUrl("/admin/applications"));

        mockMvc.perform(get("/submit"))
                .andExpect(status().isOk())
                .andExpect(view().name("submit/check"));

        mockMvc.perform(post("/submit/lookup").with(csrf())
                        .param("representativePhone", "010-0000-0000")
                        .param("password", "submit123"))
                .andExpect(status().isOk())
                .andExpect(view().name("submit/check"));

        // Result check stays view-only and redirects after POST to avoid browser resubmit prompts.
        mockMvc.perform(post("/status").with(csrf())
                        .param("representativePhone", "010-7777-8888")
                        .param("password", "submit123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/status/result"));

        // Submission lookup is a separate approved-only flow.
        mockMvc.perform(post("/submit/lookup").with(csrf())
                        .param("representativePhone", "010-7777-8888")
                        .param("password", "submit123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/submit/list"));

        // Approved + window open: the form renders with the team context filled in.
        mockMvc.perform(post("/submit").with(csrf())
                        .param("applicationId", id.toString())
                        .param("representativePhone", "010-7777-8888")
                        .param("password", "submit123"))
                .andExpect(status().isOk())
                .andExpect(view().name("submit/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Submission Team")));

        MockMultipartFile deck = new MockMultipartFile(
                "attachments", "deck.pptx", "application/vnd.ms-powerpoint", "slides".getBytes());
        mockMvc.perform(multipart("/submit/save")
                        .file(deck)
                        .param("applicationId", id.toString())
                        .param("representativePhone", "010-7777-8888")
                        .param("password", "submit123")
                        .param("summary", "Demo agent")
                        .param("demoUrl", "https://example.com")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("submit/form"))
                .andExpect(model().attribute("saved", true));

        TaskSubmission submission = submissionService.findByApplication(id).orElseThrow();
        assertThat(submission.getSummary()).isEqualTo("Demo agent");
        assertThat(submission.getFiles()).hasSize(1);
        Long fileId = submission.getFiles().get(0).getId();
        Path submittedFile = Path.of(submission.getFiles().get(0).getFilePath());
        assertThat(Files.exists(submittedFile)).isTrue();

        mockMvc.perform(get("/admin/submissions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/submission-list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/admin/submissions/" + id)));

        mockMvc.perform(get("/admin/submissions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/submission-detail"))
                .andExpect(model().attribute("hackathonApplication",
                        org.hamcrest.Matchers.hasProperty("topic",
                                org.hamcrest.Matchers.is("Submission Topic"))))
                .andExpect(model().attribute("submission",
                        org.hamcrest.Matchers.hasProperty("summary",
                                org.hamcrest.Matchers.is("Demo agent"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Submission content")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deck.pptx")));

        MvcResult fileDownloadResult = mockMvc.perform(
                        get("/admin/submissions/{id}/files/{fileId}/download", id, fileId))
                .andExpect(status().isOk())
                .andExpect(content().bytes("slides".getBytes()))
                .andReturn();
        assertThat(ContentDisposition.parse(fileDownloadResult.getResponse()
                        .getHeader(HttpHeaders.CONTENT_DISPOSITION)).getFilename())
                .isEqualTo("Submission Team_홍길동_홍길길_deck.pptx");

        MvcResult asyncArchiveResult = mockMvc.perform(
                        get("/admin/submissions/{id}/files/download-all", id))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult archiveDownloadResult = mockMvc.perform(asyncDispatch(asyncArchiveResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andReturn();
        assertThat(ContentDisposition.parse(archiveDownloadResult.getResponse()
                        .getHeader(HttpHeaders.CONTENT_DISPOSITION)).getFilename())
                .isEqualTo("Submission Team_과제제출파일.zip");
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(
                archiveDownloadResult.getResponse().getContentAsByteArray()))) {
            assertThat(zipInputStream.getNextEntry().getName()).isEqualTo("홍길동_홍길길_deck.pptx");
            assertThat(zipInputStream.readAllBytes()).isEqualTo("slides".getBytes());
            assertThat(zipInputStream.getNextEntry()).isNull();
        }

        mockMvc.perform(post("/submit/files/{fileId}/delete", fileId).with(csrf())
                        .param("applicationId", id.toString())
                        .param("representativePhone", "010-7777-8888")
                        .param("password", "submit123"))
                .andExpect(status().isOk())
                .andExpect(view().name("submit/form"));
        assertThat(Files.exists(submittedFile)).isFalse();
        assertThat(submissionService.findByApplication(id).orElseThrow().getFiles()).isEmpty();

        // Closing the result check also closes the submission window; edits are blocked.
        mockMvc.perform(post("/admin/applications/status-access").with(csrf()).param("open", "false"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(multipart("/submit/save")
                        .param("applicationId", id.toString())
                        .param("representativePhone", "010-7777-8888")
                        .param("password", "submit123")
                        .param("summary", "Changed after close")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("submit/form"));
        assertThat(submissionService.findByApplication(id).orElseThrow().getSummary())
                .isEqualTo("Demo agent");

        // Deleting the application also removes its submission (FK ordering).
        mockMvc.perform(post("/admin/applications/{id}/delete", id).with(csrf()))
                .andExpect(redirectedUrl("/admin/applications"));
        assertThat(submissionRepository.findByApplicationId(id)).isEmpty();
        assertThat(applicationRepository.count()).isZero();
    }

    @Test
    void taskSubmissionCanTargetEachApprovedApplicationForTheSamePhone() throws Exception {
        mockMvc.perform(post("/admin/applications/apply-access").with(csrf()).param("open", "true"))
                .andExpect(redirectedUrl("/admin/applications"));
        mockMvc.perform(post("/admin/applications/status-access").with(csrf()).param("open", "true"))
                .andExpect(redirectedUrl("/admin/applications"));

        mockMvc.perform(multipart("/apply")
                        .param("teamName", "First Approved Team")
                        .param("representativePhone", "010-2222-3333")
                        .param("password", "same1234")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "6000006")
                        .param("members[0].name", "Tester")
                        .param("category", "MARKETING")
                        .param("topic", "First Topic")
                        .param("content", "First content")
                        .with(csrf()))
                .andExpect(redirectedUrl("/apply/complete"));
        mockMvc.perform(multipart("/apply")
                        .param("teamName", "Second Approved Team")
                        .param("representativePhone", "010-2222-3333")
                        .param("password", "same1234")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "6000006")
                        .param("members[0].name", "Tester")
                        .param("category", "MARKETING")
                        .param("topic", "Second Topic")
                        .param("content", "Second content")
                        .with(csrf()))
                .andExpect(redirectedUrl("/apply/complete"));

        var applications = applicationService.findMyApplications("010-2222-3333", "same1234");
        Long firstId = applications.stream()
                .filter(application -> application.getTeamName().equals("First Approved Team"))
                .findFirst()
                .orElseThrow()
                .getId();
        Long secondId = applications.stream()
                .filter(application -> application.getTeamName().equals("Second Approved Team"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/admin/applications/{id}/approve", firstId).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/applications/{id}/approve", secondId).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/status").with(csrf())
                        .param("representativePhone", "010-2222-3333")
                        .param("password", "same1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/status/result"));

        mockMvc.perform(post("/submit/lookup").with(csrf())
                        .param("representativePhone", "010-2222-3333")
                        .param("password", "same1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/submit/list"));

        mockMvc.perform(multipart("/submit/save")
                        .param("applicationId", firstId.toString())
                        .param("representativePhone", "010-2222-3333")
                        .param("password", "same1234")
                        .param("summary", "First submission")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("saved", true));
        mockMvc.perform(multipart("/submit/save")
                        .param("applicationId", secondId.toString())
                        .param("representativePhone", "010-2222-3333")
                        .param("password", "same1234")
                        .param("summary", "Second submission")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("saved", true));

        assertThat(submissionService.findByApplication(firstId).orElseThrow().getSummary())
                .isEqualTo("First submission");
        assertThat(submissionService.findByApplication(secondId).orElseThrow().getSummary())
                .isEqualTo("Second submission");

        submissionRepository.deleteAll();
        applicationRepository.deleteAll();
    }

    @Test
    void applyWindowCanBeClosedByAdmin() throws Exception {
        mockMvc.perform(post("/admin/applications/apply-access").with(csrf()).param("open", "false"))
                .andExpect(redirectedUrl("/admin/applications"));

        mockMvc.perform(get("/apply"))
                .andExpect(status().isOk())
                .andExpect(view().name("apply/closed"));

        // The POST path is blocked server-side too, not just the form page.
        mockMvc.perform(multipart("/apply")
                        .param("teamName", "Late Team")
                        .param("representativePhone", "010-9999-0000")
                        .param("password", "late1234")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "5000005")
                        .param("members[0].name", "Tester")
                        .param("category", "MARKETING")
                        .param("topic", "Late Topic")
                        .param("content", "Late content")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("apply/closed"));
        assertThat(applicationRepository.count()).isZero();

        // Reopen so the shared toggle does not leak into other tests.
        mockMvc.perform(post("/admin/applications/apply-access").with(csrf()).param("open", "true"))
                .andExpect(redirectedUrl("/admin/applications"));
        mockMvc.perform(get("/apply"))
                .andExpect(status().isOk())
                .andExpect(view().name("apply/form"));
    }

    @Test
    void employeeNumberMustContainExactlySevenDigits() throws Exception {
        mockMvc.perform(multipart("/apply")
                        .param("teamName", "Invalid Team")
                        .param("representativePhone", "010-1111-2222")
                        .param("password", "test1234")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "12345")
                        .param("members[0].name", "Tester")
                        .param("topic", "Topic")
                        .param("content", "Content")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("apply/form"));
    }
}
