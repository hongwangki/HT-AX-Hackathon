package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.dto.AttachmentDownload;
import haitai.ht_ax_hackathon.service.HackathonApplicationService;
import haitai.ht_ax_hackathon.service.StatusAccessService;
import haitai.ht_ax_hackathon.service.TaskSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/submissions")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private final TaskSubmissionService submissionService;
    private final HackathonApplicationService applicationService;
    private final StatusAccessService statusAccessService;

    /** Approved teams only, each with its submission (or lack of one). */
    @GetMapping
    public String submissionList(Model model) {
        List<HackathonApplication> approved = submissionService.findApprovedApplications();
        Map<Long, TaskSubmission> submissions = submissionService.findSubmissionsFor(approved);
        model.addAttribute("approvedApplications", approved);
        model.addAttribute("submissions", submissions);
        model.addAttribute("submittedCount", submissions.size());
        model.addAttribute("submissionOpen", statusAccessService.isSubmissionOpen());
        return "admin/submission-list";
    }

    /** Shows the original approved task and the submitted result together. */
    @GetMapping("/{applicationId}")
    public String submissionDetail(@PathVariable Long applicationId, Model model) {
        HackathonApplication application = applicationService.findApplication(applicationId);
        TaskSubmission submission = submissionService.findByApplication(applicationId).orElse(null);
        model.addAttribute("hackathonApplication", application);
        model.addAttribute("submission", submission);
        return "admin/submission-detail";
    }

    @GetMapping("/{applicationId}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long applicationId, @PathVariable Long fileId) {
        AttachmentDownload download = submissionService.getFileDownload(applicationId, fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(resolveContentType(download.contentType()))
                .body(download.resource());
    }

    private MediaType resolveContentType(String contentType) {
        try {
            return contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
