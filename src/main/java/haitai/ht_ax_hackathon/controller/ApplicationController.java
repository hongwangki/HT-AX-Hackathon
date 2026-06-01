package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.dto.ApplicationForm;
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

@Controller
@RequiredArgsConstructor
public class ApplicationController {

    private final HackathonApplicationService applicationService;

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
}
