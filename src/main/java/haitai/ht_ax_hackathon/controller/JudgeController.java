package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.domain.JudgeEvaluationStatus;
import haitai.ht_ax_hackathon.dto.AttachmentDownload;
import haitai.ht_ax_hackathon.dto.JudgeEvaluationDetail;
import haitai.ht_ax_hackathon.dto.JudgeEvaluationForm;
import haitai.ht_ax_hackathon.dto.JudgeSubmissionListItem;
import haitai.ht_ax_hackathon.service.JudgeEvaluationService;
import haitai.ht_ax_hackathon.service.TaskSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeEvaluationService evaluationService;
    private final TaskSubmissionService submissionService;

    @GetMapping("/judge/login")
    public String loginPage() {
        return "judge/login";
    }

    @GetMapping("/judge/evaluations")
    public String evaluationList(Principal principal, Model model) {
        List<JudgeSubmissionListItem> targets = evaluationService.findEvaluationTargets(principal.getName());
        long submittedCount = targets.stream()
                .filter(item -> item.evaluationStatus() == JudgeEvaluationStatus.SUBMITTED)
                .count();
        long draftCount = targets.stream()
                .filter(item -> item.evaluationStatus() == JudgeEvaluationStatus.DRAFT)
                .count();

        model.addAttribute("judge", evaluationService.findJudge(principal.getName()));
        model.addAttribute("targets", targets);
        model.addAttribute("ranking", evaluationService.buildRanking(targets));
        model.addAttribute("submittedCount", submittedCount);
        model.addAttribute("draftCount", draftCount);
        return "judge/evaluation-list";
    }

    @GetMapping("/judge/evaluations/{applicationId}")
    public String evaluationDetail(@PathVariable Long applicationId, Principal principal) {
        evaluationService.findEvaluationDetail(principal.getName(), applicationId);
        return "redirect:/judge/evaluations?applicationId=" + applicationId;
    }

    @GetMapping("/judge/evaluations/{applicationId}/panel")
    public String evaluationPanel(@PathVariable Long applicationId, Principal principal, Model model) {
        JudgeEvaluationDetail detail = evaluationService
                .findEvaluationDetail(principal.getName(), applicationId);
        JudgeEvaluationForm form = detail.evaluation()
                .map(JudgeEvaluationForm::from)
                .orElseGet(JudgeEvaluationForm::new);
        model.addAttribute("evaluationForm", form);
        populateDetailModel(principal, detail, model);
        return "judge/evaluation-detail :: evaluationPanel";
    }

    @PostMapping("/judge/evaluations/{applicationId}")
    public String saveEvaluation(
            @PathVariable Long applicationId,
            @RequestParam(defaultValue = "DRAFT") String action,
            @Valid @ModelAttribute("evaluationForm") JudgeEvaluationForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model
    ) {
        JudgeEvaluationStatus requestedStatus = parseStatus(action, bindingResult);
        if (requestedStatus == JudgeEvaluationStatus.SUBMITTED && !form.isComplete()) {
            bindingResult.reject("scores.required", "평가 완료 전 모든 항목의 점수를 입력해 주세요.");
        }

        if (!bindingResult.hasErrors()) {
            try {
                evaluationService.saveEvaluation(
                        principal.getName(), applicationId, form, requestedStatus
                );
                String result = requestedStatus == JudgeEvaluationStatus.SUBMITTED
                        ? "submitted"
                        : "saved";
                return "redirect:/judge/evaluations?applicationId=" + applicationId + "&" + result;
            } catch (IllegalArgumentException | IllegalStateException exception) {
                bindingResult.reject("evaluation.save", exception.getMessage());
            }
        }

        JudgeEvaluationDetail detail = evaluationService
                .findEvaluationDetail(principal.getName(), applicationId);
        populateDetailModel(principal, detail, model);
        return "judge/evaluation-detail";
    }

    @GetMapping("/judge/evaluations/{applicationId}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long applicationId,
            @PathVariable Long fileId,
            Principal principal
    ) {
        evaluationService.findEvaluationDetail(principal.getName(), applicationId);
        AttachmentDownload download = submissionService.getJudgeFileDownload(applicationId, fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(resolveContentType(download.contentType()))
                .body(download.resource());
    }

    private void populateDetailModel(Principal principal, JudgeEvaluationDetail detail, Model model) {
        model.addAttribute("judge", evaluationService.findJudge(principal.getName()));
        model.addAttribute("submission", detail.submission());
        model.addAttribute("hackathonApplication", detail.submission().getApplication());
        model.addAttribute("evaluation", detail.evaluation().orElse(null));
        model.addAttribute("guideOverview", detail.guideOverview().orElse(null));
        model.addAttribute("guideItems", detail.guideItems());
        model.addAttribute("evaluationLocked", detail.evaluation()
                .map(item -> item.getStatus() == JudgeEvaluationStatus.SUBMITTED)
                .orElse(false));
    }

    private JudgeEvaluationStatus parseStatus(String action, BindingResult bindingResult) {
        try {
            return JudgeEvaluationStatus.valueOf(action);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("evaluation.action", "올바르지 않은 저장 요청입니다.");
            return JudgeEvaluationStatus.DRAFT;
        }
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
