package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.dto.AdminFinalResultDetail;
import haitai.ht_ax_hackathon.dto.AdminFinalResultSummary;
import haitai.ht_ax_hackathon.service.AdminFinalResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/final-results")
@RequiredArgsConstructor
public class AdminFinalResultController {

    private final AdminFinalResultService finalResultService;

    @GetMapping
    public String finalResultList(Model model) {
        List<AdminFinalResultSummary> results = finalResultService.findAllResults();
        model.addAttribute("results", results);
        model.addAttribute("targetCount", results.size());
        model.addAttribute("activeJudgeCount", finalResultService.countActiveJudges());
        model.addAttribute("confirmedCount", results.stream().filter(AdminFinalResultSummary::confirmed).count());
        model.addAttribute("pendingCount", results.stream().filter(result -> !result.confirmed()).count());
        return "admin/final-result-list";
    }

    @GetMapping("/{applicationId}")
    public String finalResultDetail(@PathVariable Long applicationId, Model model) {
        AdminFinalResultDetail result = finalResultService.findResultDetail(applicationId);
        model.addAttribute("result", result);
        return "admin/final-result-detail";
    }
}
