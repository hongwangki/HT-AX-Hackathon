package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.domain.ApplicationCategory;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.dto.ApplicationForm;
import haitai.ht_ax_hackathon.dto.StatusCheckForm;
import haitai.ht_ax_hackathon.exception.FileStorageException;
import haitai.ht_ax_hackathon.service.HackathonApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ApplicationController {

    private final HackathonApplicationService applicationService;

    /** Select-box options for the apply form, available on every render including validation errors. */
    @ModelAttribute("categories")
    public ApplicationCategory[] categories() {
        return ApplicationCategory.values();
    }

    @GetMapping("/apply")
    public String applicationForm(Model model) {
        model.addAttribute("applicationForm", new ApplicationForm());
        return "apply/form";
    }

    @PostMapping("/apply")
    public String submitApplication(
            @Valid @ModelAttribute("applicationForm") ApplicationForm form,
            BindingResult bindingResult
    ) {
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

    /** Renders the result directly (no redirect) so credentials never appear in the URL. */
    @PostMapping("/status")
    public String checkStatus(
            @Valid @ModelAttribute("statusCheckForm") StatusCheckForm form,
            BindingResult bindingResult,
            Model model
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

        model.addAttribute("applications", applications);
        return "status/list";
    }
}
