package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.dto.StatusCheckForm;
import haitai.ht_ax_hackathon.dto.TaskSubmissionForm;
import haitai.ht_ax_hackathon.exception.FileStorageException;
import haitai.ht_ax_hackathon.exception.TaskSubmissionSizeExceededException;
import haitai.ht_ax_hackathon.service.StatusAccessService;
import haitai.ht_ax_hackathon.service.TaskSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Task submission flow for approved applicants. Save/delete endpoints re-verify the
 * phone+password+application id tuple so one phone number can submit for multiple approved
 * applications without exposing credentials in URLs.
 */
@Controller
@RequestMapping("/submit")
@RequiredArgsConstructor
public class TaskSubmissionController {

    private final TaskSubmissionService submissionService;
    private final StatusAccessService statusAccessService;

    @GetMapping
    public String submissionCheckForm(Model model) {
        model.addAttribute("statusCheckForm", new StatusCheckForm());
        return "submit/check";
    }

    @GetMapping("/list")
    public String submissionList(Model model) {
        if (!model.containsAttribute("applications") || !model.containsAttribute("statusCheckForm")) {
            return "redirect:/submit";
        }
        addSubmissionListModel(model);
        return "submit/list";
    }

    @PostMapping("/lookup")
    public String lookupApprovedApplications(
            @Valid @ModelAttribute("statusCheckForm") StatusCheckForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "submit/check";
        }

        var approvedApplications =
                submissionService.findApprovedApplications(form.getRepresentativePhone(), form.getPassword());
        if (approvedApplications.isEmpty()) {
            bindingResult.reject("submission.notApproved",
                    "승인된 신청 내역이 없습니다. 결과확인에서 진행상태를 확인해 주세요.");
            return "submit/check";
        }

        redirectAttributes.addFlashAttribute("statusCheckForm", form);
        redirectAttributes.addFlashAttribute("applications", approvedApplications);
        return "redirect:/submit/list";
    }

    /** Entry from the status page: shows the submission form for the approved application. */
    @PostMapping
    public String submissionForm(
            @RequestParam Long applicationId,
            @RequestParam String representativePhone,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<HackathonApplication> application =
                submissionService.findApprovedApplication(applicationId, representativePhone, password);
        if (application.isEmpty()) {
            return rejectAccess(redirectAttributes);
        }

        TaskSubmissionForm form = new TaskSubmissionForm();
        form.setApplicationId(applicationId);
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
                submissionService.findApprovedApplication(
                        form.getApplicationId(), form.getRepresentativePhone(), form.getPassword());
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
        } catch (TaskSubmissionSizeExceededException exception) {
            bindingResult.reject("file.upload.tooLarge", exception.getMessage());
        }

        addFormModel(model, application.get(), form);
        return "submit/form";
    }

    @PostMapping("/files/{fileId}/delete")
    public String deleteFile(
            @PathVariable Long fileId,
            @RequestParam Long applicationId,
            @RequestParam String representativePhone,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<HackathonApplication> application =
                submissionService.findApprovedApplication(applicationId, representativePhone, password);
        if (application.isEmpty()) {
            return rejectAccess(redirectAttributes);
        }

        if (statusAccessService.isSubmissionOpen()) {
            submissionService.deleteFile(application.get().getId(), fileId);
        }

        TaskSubmissionForm form = new TaskSubmissionForm();
        form.setApplicationId(applicationId);
        form.setRepresentativePhone(representativePhone);
        form.setPassword(password);
        prefillDetails(application.get().getId(), form);
        addFormModel(model, application.get(), form);
        return "submit/form";
    }

    /** Failed re-verification sends the user back to the status check entry point. */
    private String rejectAccess(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("flashError",
                "선택한 승인 신청 내역을 찾을 수 없습니다. 신청현황 조회 후 다시 시도해 주세요.");
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

    @SuppressWarnings("unchecked")
    private void addSubmissionListModel(Model model) {
        var applications = (java.util.List<HackathonApplication>) model.asMap().get("applications");
        model.addAttribute("submissionOpen", statusAccessService.isSubmissionOpen());
        model.addAttribute("submissionsByApplicationId", submissionService.findSubmissionsFor(applications));
    }
}
