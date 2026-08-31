package haitai.ht_ax_hackathon.domain;

public enum JudgeEvaluationStatus {
    DRAFT("임시 저장"),
    SUBMITTED("평가 완료");

    private final String label;

    JudgeEvaluationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
