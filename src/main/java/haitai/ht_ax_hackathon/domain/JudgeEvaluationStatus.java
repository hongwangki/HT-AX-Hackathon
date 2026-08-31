package haitai.ht_ax_hackathon.domain;

public enum JudgeEvaluationStatus {
    /** 네 항목 중 일부만 입력된 상태입니다. 저장 여부와 무관하게 언제든 고칠 수 있습니다. */
    DRAFT("작성 중"),
    SUBMITTED("평가 완료");

    private final String label;

    JudgeEvaluationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
