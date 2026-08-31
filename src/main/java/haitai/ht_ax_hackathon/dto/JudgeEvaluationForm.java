package haitai.ht_ax_hackathon.dto;

import haitai.ht_ax_hackathon.domain.JudgeEvaluation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JudgeEvaluationForm {

    @Min(value = 0, message = "0점 이상 입력해 주세요.")
    @Max(value = 30, message = "경영효과는 30점을 초과할 수 없습니다.")
    private Integer managementEffectScore;

    @Min(value = 0, message = "0점 이상 입력해 주세요.")
    @Max(value = 30, message = "현업 활용 가능성은 30점을 초과할 수 없습니다.")
    private Integer fieldUsabilityScore;

    @Min(value = 0, message = "0점 이상 입력해 주세요.")
    @Max(value = 20, message = "타부서 적용 가능성은 20점을 초과할 수 없습니다.")
    private Integer expandabilityScore;

    @Min(value = 0, message = "0점 이상 입력해 주세요.")
    @Max(value = 20, message = "혁신·창의성은 20점을 초과할 수 없습니다.")
    private Integer innovationScore;

    public static JudgeEvaluationForm from(JudgeEvaluation evaluation) {
        JudgeEvaluationForm form = new JudgeEvaluationForm();
        form.setManagementEffectScore(evaluation.getManagementEffectScore());
        form.setFieldUsabilityScore(evaluation.getFieldUsabilityScore());
        form.setExpandabilityScore(evaluation.getExpandabilityScore());
        form.setInnovationScore(evaluation.getInnovationScore());
        return form;
    }

    public int totalScore() {
        return valueOrZero(managementEffectScore)
                + valueOrZero(fieldUsabilityScore)
                + valueOrZero(expandabilityScore)
                + valueOrZero(innovationScore);
    }

    public boolean isComplete() {
        return managementEffectScore != null
                && fieldUsabilityScore != null
                && expandabilityScore != null
                && innovationScore != null;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
