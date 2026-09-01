package haitai.ht_ax_hackathon.repository;

import haitai.ht_ax_hackathon.domain.JudgeEvaluation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JudgeEvaluationRepository extends JpaRepository<JudgeEvaluation, Long> {

    Optional<JudgeEvaluation> findByJudgeIdAndApplicationId(Long judgeId, Long applicationId);

    @EntityGraph(attributePaths = "application")
    List<JudgeEvaluation> findByJudgeId(Long judgeId);

    @EntityGraph(attributePaths = {"judge", "application"})
    @Query("""
            select evaluation
            from JudgeEvaluation evaluation
            where evaluation.application.id in :applicationIds
              and evaluation.judge.id in :judgeIds
            """)
    List<JudgeEvaluation> findForFinalResults(
            @Param("applicationIds") List<Long> applicationIds,
            @Param("judgeIds") List<Long> judgeIds
    );
}
