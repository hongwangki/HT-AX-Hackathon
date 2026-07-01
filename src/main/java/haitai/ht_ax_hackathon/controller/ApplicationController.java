package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.domain.ApplicationCategory;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.dto.ApplicationForm;
import haitai.ht_ax_hackathon.dto.StatusCheckForm;
import haitai.ht_ax_hackathon.exception.FileStorageException;
import haitai.ht_ax_hackathon.service.HackathonApplicationService;
import haitai.ht_ax_hackathon.service.StatusAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ApplicationController {

    private final HackathonApplicationService applicationService;
    private final StatusAccessService statusAccessService;

    /** Select-box options for the apply form, available on every render including validation errors. */
    @ModelAttribute("categories")
    public ApplicationCategory[] categories() {
        return ApplicationCategory.values();
    }

    @GetMapping("/apply")
    public String applicationForm(Model model) {
        if (!statusAccessService.isApplyOpen()) {
            return "apply/closed";
        }
        model.addAttribute("applicationForm", new ApplicationForm());
        return "apply/form";
    }

    @PostMapping("/apply")
    public String submitApplication(
            @Valid @ModelAttribute("applicationForm") ApplicationForm form,
            BindingResult bindingResult
    ) {
        if (!statusAccessService.isApplyOpen()) {
            return "apply/closed";
        }
        // Checked here instead of via @NotBlank because the admin edit screen shares this form.
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "password.required", "신청현황 조회에 사용할 비밀번호를 입력해 주세요.");
        } else if (form.getPassword().length() < 4) {
            bindingResult.rejectValue("password", "password.tooShort", "비밀번호는 4자 이상 입력해 주세요.");
        } else if (!bindingResult.hasFieldErrors("representativePhone")
                && applicationService.passwordConflictsWithExisting(form.getRepresentativePhone(), form.getPassword())) {
            bindingResult.rejectValue("password", "password.mismatch",
                    "이 전화번호로 신청한 내역이 이미 있습니다. 처음 신청할 때 사용한 비밀번호를 입력해 주세요.");
        }

        if (bindingResult.hasErrors()) {
            return "apply/form";
        }

        try {
            applicationService.createApplication(form);
        } catch (FileStorageException exception) {
            bindingResult.reject("file.upload.failed", exception.getMessage());
            return "apply/form";
        }

        return "redirect:/apply/complete";
    }

    @GetMapping("/apply/complete")
    public String complete() {
        return "apply/complete";
    }

    @GetMapping("/status")
    public String statusCheckForm(Model model) {
        model.addAttribute("statusCheckForm", new StatusCheckForm());
        return "status/check";
    }

    @GetMapping("/status/result")
    public String statusResult(Model model) {
        if (!model.containsAttribute("applications") || !model.containsAttribute("statusCheckForm")) {
            return "redirect:/status";
        }
        model.addAttribute("applyOpen", statusAccessService.isApplyOpen());
        return "status/list";
    }

    /** Redirects after a successful lookup so browser back/refresh never resubmits the form. */
    @PostMapping("/status")
    public String checkStatus(
            @Valid @ModelAttribute("statusCheckForm") StatusCheckForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "status/check";
        }

        List<HackathonApplication> applications =
                applicationService.findMyApplications(form.getRepresentativePhone(), form.getPassword());

        if (applications.isEmpty()) {
            bindingResult.reject("status.notFound", "일치하는 신청 내역이 없습니다. 전화번호와 비밀번호를 확인해 주세요.");
            return "status/check";
        }

        redirectAttributes.addFlashAttribute("statusCheckForm", form);
        redirectAttributes.addFlashAttribute("applications", applications);
        return "redirect:/status/result";
    }
}
