package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.dto.TaskSubmissionForm;
import haitai.ht_ax_hackathon.exception.FileStorageException;
import haitai.ht_ax_hackathon.service.StatusAccessService;
import haitai.ht_ax_hackathon.service.TaskSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Task submission flow for approved applicants. Every endpoint is a POST that re-verifies
 * the phone+password pair, so credentials never appear in URLs and no session state is needed.
 * The only way in is the button on the status page, which forwards the verified credentials.
 */
@Controller
@RequestMapping("/submit")
@RequiredArgsConstructor
public class TaskSubmissionController {

    private final TaskSubmissionService submissionService;
    private final StatusAccessService statusAccessService;

    /** Entry from the status page: shows the submission form for the approved application. */
    @PostMapping
    public String submissionForm(
            @RequestParam String representativePhone,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<HackathonApplication> application =
                submissionService.findApprovedApplication(representativePhone, password);
        if (application.isEmpty()) {
            return rejectAccess(redirectAttributes);
        }

        TaskSubmissionForm form = new TaskSubmissionForm();
        form.setRepresentativePhone(representativePhone);
        form.setPassword(password);
        prefillDetails(application.get().getId(), form);
        addFormModel(model, application.get(), form);
        return "submit/form";
    }

    @PostMapping("/save")
    public String saveSubmission(
            @Valid @ModelAttribute("submissionForm") TaskSubmissionForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<HackathonApplication> application =
                submissionService.findApprovedApplication(form.getRepresentativePhone(), form.getPassword());
        if (application.isEmpty()) {
            return rejectAccess(redirectAttributes);
        }

        if (!statusAccessService.isSubmissionOpen()) {
            bindingResult.reject("submission.closed", "과제 제출 기간이 아닙니다.");
        }
        if (bindingResult.hasErrors()) {
            addFormModel(model, application.get(), form);
            return "submit/form";
        }

        try {
            submissionService.saveSubmission(
                    application.get().getId(), form.getSummary(), form.getDemoUrl(), form.getAttachments());
            model.addAttribute("saved", true);
        } catch (FileStorageException exception) {
            bindingResult.reject("file.upload.failed", exception.getMessage());
        }

        addFormModel(model, application.get(), form);
        return "submit/form";
    }

    @PostMapping("/files/{fileId}/delete")
    public String deleteFile(
            @PathVariable Long fileId,
            @RequestParam String representativePhone,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<HackathonApplication> application =
                submissionService.findApprovedApplication(representativePhone, password);
        if (application.isEmpty()) {
            return rejectAccess(redirectAttributes);
        }

        if (statusAccessService.isSubmissionOpen()) {
            submissionService.deleteFile(application.get().getId(), fileId);
        }

        TaskSubmissionForm form = new TaskSubmissionForm();
        form.setRepresentativePhone(representativePhone);
        form.setPassword(password);
        prefillDetails(application.get().getId(), form);
        addFormModel(model, application.get(), form);
        return "submit/form";
    }

    /** Failed re-verification sends the user back to the status check entry point. */
    private String rejectAccess(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("flashError",
                "승인된 신청 내역을 찾을 수 없습니다. 신청현황 조회 후 다시 시도해 주세요.");
        return "redirect:/status";
    }

    private void prefillDetails(Long applicationId, TaskSubmissionForm form) {
        submissionService.findByApplication(applicationId).ifPresent(submission -> {
            form.setSummary(submission.getSummary());
            form.setDemoUrl(submission.getDemoUrl());
        });
    }

    private void addFormModel(Model model, HackathonApplication application, TaskSubmissionForm form) {
        // "application" is a reserved Thymeleaf implicit object (ServletContext), so use a distinct name.
        model.addAttribute("hackathonApplication", application);
        model.addAttribute("submissionForm", form);
        model.addAttribute("submission", submissionService.findByApplication(application.getId()).orElse(null));
        model.addAttribute("submissionOpen", statusAccessService.isSubmissionOpen());
    }
}
