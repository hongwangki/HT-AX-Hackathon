package haitai.ht_ax_hackathon.domain;

public enum ApplicationStatus {
    SUBMITTED("접수"),
    REVIEWING("검토 중"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
