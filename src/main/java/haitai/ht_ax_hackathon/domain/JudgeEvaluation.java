package haitai.ht_ax_hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "judge_evaluations",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_judge_evaluations_judge_application",
                columnNames = {"judge_id", "application_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeEvaluation {

    @Id
    @SequenceGenerator(
            name = "judge_evaluations_seq",
            sequenceName = "seq_judge_evaluations",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "judge_evaluations_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "judge_id", nullable = false)
    private Judge judge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private HackathonApplication application;

    private Integer managementEffectScore;
    private Integer fieldUsabilityScore;
    private Integer expandabilityScore;
    private Integer innovationScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JudgeEvaluationStatus status = JudgeEvaluationStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;

    public JudgeEvaluation(Judge judge, HackathonApplication application) {
        this.judge = judge;
        this.application = application;
    }

    /**
     * 심사자는 언제든 점수를 고칠 수 있습니다. 네 항목이 모두 채워지면 평가 완료로 보고,
     * 하나라도 비어 있으면 임시 저장 상태로 되돌립니다. 별도의 완료 버튼은 없습니다.
     */
    public void saveScores(Integer managementEffectScore, Integer fieldUsabilityScore,
                           Integer expandabilityScore, Integer innovationScore) {
        this.managementEffectScore = managementEffectScore;
        this.fieldUsabilityScore = fieldUsabilityScore;
        this.expandabilityScore = expandabilityScore;
        this.innovationScore = innovationScore;

        boolean complete = managementEffectScore != null && fieldUsabilityScore != null
                && expandabilityScore != null && innovationScore != null;
        this.status = complete ? JudgeEvaluationStatus.SUBMITTED : JudgeEvaluationStatus.DRAFT;
        this.submittedAt = complete ? LocalDateTime.now() : null;
    }

    public int getTotalScore() {
        return scoreOrZero(managementEffectScore)
                + scoreOrZero(fieldUsabilityScore)
                + scoreOrZero(expandabilityScore)
                + scoreOrZero(innovationScore);
    }

    private int scoreOrZero(Integer score) {
        return score == null ? 0 : score;
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
