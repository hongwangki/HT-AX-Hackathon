package haitai.ht_ax_hackathon.domain;

public enum ApplicationCategory {
    MARKETING("마케팅"),
    LOGISTICS("물류"),
    PRODUCTION("생산"),
    SALES("영업"),
    GENERAL_MANAGEMENT("일반관리"),
    PRODUCT_DEVELOPMENT("제품개발"),
    ETC("기타");

    private final String label;

    ApplicationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
