package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.domain.ApplicationStatus;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import haitai.ht_ax_hackathon.service.HackathonApplicationService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@SpringBootTest
class HtAxHackathonApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HackathonApplicationRepository applicationRepository;

    @Autowired
    private HackathonApplicationService applicationService;

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
                        .param("content", "Test application content"))
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

        mockMvc.perform(post("/admin/applications/{id}/approve", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/applications/" + id));
        assertThat(applicationService.findApplication(id).getStatus()).isEqualTo(ApplicationStatus.APPROVED);

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

        mockMvc.perform(post("/admin/applications/{id}/reject", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/applications/" + id));
        assertThat(applicationService.findApplication(id).getStatus()).isEqualTo(ApplicationStatus.REJECTED);

        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("status/check"));

        mockMvc.perform(post("/status")
                        .param("representativePhone", "010-1234-5678")
                        .param("password", "test1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("status/list"));

        mockMvc.perform(post("/status")
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

        mockMvc.perform(post("/admin/applications/{id}/files/{fileId}/delete", id, fileId))
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
                        .param("content", "Updated content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/applications/" + id));

        assertThat(applicationService.findApplication(id).getTeamName()).isEqualTo("Updated AX Team");

        mockMvc.perform(get("/admin/applications/{id}/delete", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/application-delete"));

        mockMvc.perform(post("/admin/applications/{id}/delete", id))
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
                        .param("content", "First content"))
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
                        .param("content", "Second content"))
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
                        .param("content", "Second content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/apply/complete"));

        // One phone + password pair returns the full submission history.
        assertThat(applicationService.findMyApplications("010-5555-6666", "first123")).hasSize(2);
        assertThat(applicationService.findMyApplications("010-5555-6666", "different999")).isEmpty();

        // Admin looks the stored password up by phone number to relay it to the applicant.
        mockMvc.perform(post("/admin/applications/password-lookup")
                        .param("representativePhone", "010-5555-6666"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/password-lookup"))
                .andExpect(model().attribute("foundPassword", "first123"));
        assertThat(applicationService.findPasswordByPhone("010-5555-6666")).contains("first123");

        // Looking up an unknown phone number stays on the form with an error.
        mockMvc.perform(post("/admin/applications/password-lookup")
                        .param("representativePhone", "010-0000-0000"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/password-lookup"))
                .andExpect(model().attributeDoesNotExist("foundPassword"));

        applicationRepository.deleteAll();
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
                        .param("content", "Content"))
                .andExpect(status().isOk())
                .andExpect(view().name("apply/form"));
    }
}
