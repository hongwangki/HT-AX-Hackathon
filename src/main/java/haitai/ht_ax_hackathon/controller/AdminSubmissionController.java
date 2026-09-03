package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.dto.AttachmentDownload;
import haitai.ht_ax_hackathon.dto.SubmissionArchiveDownload;
import haitai.ht_ax_hackathon.exception.AttachmentFileNotFoundException;
import haitai.ht_ax_hackathon.exception.FileStorageException;
import haitai.ht_ax_hackathon.exception.TaskSubmissionSizeExceededException;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @PostMapping("/{applicationId}/files")
    public String addFiles(
            @PathVariable Long applicationId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            RedirectAttributes redirectAttributes
    ) {
        try {
            submissionService.addFiles(applicationId, files);
            redirectAttributes.addFlashAttribute("submissionFileMessage", "파일을 추가했습니다.");
        } catch (IllegalArgumentException | FileStorageException
                 | TaskSubmissionSizeExceededException | AttachmentFileNotFoundException exception) {
            redirectAttributes.addFlashAttribute("submissionFileError", exception.getMessage());
        }
        return "redirect:/admin/submissions/" + applicationId;
    }

    @PostMapping("/{applicationId}/files/{fileId}/delete")
    public String deleteFile(
            @PathVariable Long applicationId,
            @PathVariable Long fileId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            submissionService.deleteFile(applicationId, fileId);
            redirectAttributes.addFlashAttribute("submissionFileMessage", "파일을 삭제했습니다.");
        } catch (AttachmentFileNotFoundException exception) {
            redirectAttributes.addFlashAttribute("submissionFileError", exception.getMessage());
        }
        return "redirect:/admin/submissions/" + applicationId;
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

    @GetMapping("/{applicationId}/files/download-all")
    public ResponseEntity<StreamingResponseBody> downloadAllFiles(@PathVariable Long applicationId) {
        SubmissionArchiveDownload download = submissionService.getAllFilesDownload(applicationId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.archiveFileName(), StandardCharsets.UTF_8)
                .build();
        StreamingResponseBody body = outputStream -> {
            Set<String> usedEntryNames = new HashSet<>();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                for (AttachmentDownload file : download.files()) {
                    String entryName = createUniqueEntryName(file.originalFileName(), usedEntryNames);
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    try (InputStream inputStream = file.resource().getInputStream()) {
                        inputStream.transferTo(zipOutputStream);
                    }
                    zipOutputStream.closeEntry();
                }
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(body);
    }

    private String createUniqueEntryName(String fileName, Set<String> usedEntryNames) {
        if (usedEntryNames.add(fileName)) {
            return fileName;
        }
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        String extension = extensionIndex > 0 ? fileName.substring(extensionIndex) : "";
        int suffix = 2;
        String candidate;
        do {
            candidate = baseName + " (" + suffix++ + ")" + extension;
        } while (!usedEntryNames.add(candidate));
        return candidate;
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
