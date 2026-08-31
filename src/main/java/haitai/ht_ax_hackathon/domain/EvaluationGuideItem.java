package haitai.ht_ax_hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "evaluation_guide_items",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_evaluation_guide_items_application_criterion",
                columnNames = {"application_id", "criterion_code"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationGuideItem {

    @Id
    @SequenceGenerator(
            name = "evaluation_guide_items_seq",
            sequenceName = "seq_evaluation_guide_items",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evaluation_guide_items_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private HackathonApplication application;

    @Column(nullable = false, length = 40)
    private String criterionCode;

    @Nationalized
    @Column(nullable = false, length = 100)
    private String criterionName;

    @Column(nullable = false)
    private int maxScore;

    private Integer rawLevel;
    private Integer weightedScore;

    @Nationalized
    @Column(length = 1000)
    private String criterionText;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String detailComment;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public EvaluationGuideItem(HackathonApplication application, String criterionCode,
                               String criterionName, int maxScore, Integer rawLevel,
                               Integer weightedScore, String criterionText,
                               String detailComment, int displayOrder) {
        this.application = application;
        this.criterionCode = criterionCode;
        this.criterionName = criterionName;
        this.maxScore = maxScore;
        this.rawLevel = rawLevel;
        this.weightedScore = weightedScore;
        this.criterionText = criterionText;
        this.detailComment = detailComment;
        this.displayOrder = displayOrder;
    }

    public void update(String criterionName, int maxScore, Integer rawLevel,
                       Integer weightedScore, String criterionText,
                       String detailComment, int displayOrder) {
        this.criterionName = criterionName;
        this.maxScore = maxScore;
        this.rawLevel = rawLevel;
        this.weightedScore = weightedScore;
        this.criterionText = criterionText;
        this.detailComment = detailComment;
        this.displayOrder = displayOrder;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
