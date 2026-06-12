package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.service.StatusAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final StatusAccessService statusAccessService;

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("statusCheckOpen", statusAccessService.isStatusCheckOpen());
        model.addAttribute("applyOpen", statusAccessService.isApplyOpen());
        // Open Graph tags need absolute URLs; build them from however the site was reached.
        model.addAttribute("baseUrl",
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
        return "index";
    }
}
