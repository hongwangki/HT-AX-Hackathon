package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import haitai.ht_ax_hackathon.service.HackathonApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "1000001")
                        .param("members[0].name", "Tester")
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

        mockMvc.perform(get("/admin/applications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/application-detail"));

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
                        .param("members[0].department", "Updated Team")
                        .param("members[0].employeeNo", "2000002")
                        .param("members[0].name", "Updated Tester")
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
    void employeeNumberMustContainExactlySevenDigits() throws Exception {
        mockMvc.perform(multipart("/apply")
                        .param("teamName", "Invalid Team")
                        .param("members[0].department", "AX Team")
                        .param("members[0].employeeNo", "12345")
                        .param("members[0].name", "Tester")
                        .param("topic", "Topic")
                        .param("content", "Content"))
                .andExpect(status().isOk())
                .andExpect(view().name("apply/form"));
    }
}
