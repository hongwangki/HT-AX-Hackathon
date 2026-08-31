package haitai.ht_ax_hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_guide_overviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationGuideOverview {

    @Id
    @Column(name = "application_id")
    private Long applicationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private HackathonApplication application;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String oneLineSummary;

    private Integer sourceTotalScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public EvaluationGuideOverview(HackathonApplication application, String oneLineSummary,
                                   Integer sourceTotalScore) {
        this.application = application;
        this.oneLineSummary = oneLineSummary;
        this.sourceTotalScore = sourceTotalScore;
    }

    public void update(String oneLineSummary, Integer sourceTotalScore) {
        this.oneLineSummary = oneLineSummary;
        this.sourceTotalScore = sourceTotalScore;
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
