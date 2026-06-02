package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.dto.ApplicationForm;
import haitai.ht_ax_hackathon.dto.AttachmentDownload;
import haitai.ht_ax_hackathon.dto.ExcelDownload;
import haitai.ht_ax_hackathon.exception.FileStorageException;
import haitai.ht_ax_hackathon.service.ApplicationExcelExportService;
import haitai.ht_ax_hackathon.service.HackathonApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin/applications")
@RequiredArgsConstructor
public class AdminApplicationController {

    private final HackathonApplicationService applicationService;
    private final ApplicationExcelExportService excelExportService;

    @GetMapping
    public String applicationList(Model model) {
        List<HackathonApplication> applications = applicationService.findAllApplications();
        model.addAttribute("applications", applications);
        model.addAttribute("applicationCount", applications.size());
        return "admin/application-list";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportApplications() {
        ExcelDownload download = excelExportService.createDownload();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(download.content());
    }

    @GetMapping("/{id}")
    public String applicationDetail(@PathVariable Long id, Model model) {
        model.addAttribute("hackathonApplication", applicationService.findApplication(id));
        return "admin/application-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("applicationId", id);
        model.addAttribute("applicationForm", applicationService.getEditForm(id));
        model.addAttribute("existingFiles", applicationService.findApplication(id).getFiles());
        return "admin/application-edit";
    }

    @PostMapping("/{id}/edit")
    public String editApplication(
            @PathVariable Long id,
            @Valid @ModelAttribute("applicationForm") ApplicationForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            addEditModel(id, model);
            return "admin/application-edit";
        }

        try {
            applicationService.updateApplication(id, form);
        } catch (FileStorageException exception) {
            bindingResult.reject("file.upload.failed", exception.getMessage());
            addEditModel(id, model);
            return "admin/application-edit";
        }
        return "redirect:/admin/applications/" + id;
    }

    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id, @PathVariable Long fileId) {
        AttachmentDownload download = applicationService.getAttachmentDownload(id, fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(resolveContentType(download.contentType()))
                .body(download.resource());
    }

    @PostMapping("/{id}/files/{fileId}/delete")
    public String deleteAttachment(@PathVariable Long id, @PathVariable Long fileId) {
        applicationService.deleteAttachment(id, fileId);
        return "redirect:/admin/applications/" + id + "/edit";
    }

    @GetMapping("/{id}/delete")
    public String deleteConfirmation(@PathVariable Long id, Model model) {
        model.addAttribute("hackathonApplication", applicationService.findApplication(id));
        return "admin/application-delete";
    }

    @PostMapping("/{id}/delete")
    public String deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return "redirect:/admin/applications";
    }

    private void addEditModel(Long id, Model model) {
        model.addAttribute("applicationId", id);
        model.addAttribute("existingFiles", applicationService.findApplication(id).getFiles());
    }

    private MediaType resolveContentType(String contentType) {
        try {
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
