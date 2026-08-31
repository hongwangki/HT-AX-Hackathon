package haitai.ht_ax_hackathon.repository;

import haitai.ht_ax_hackathon.domain.JudgeEvaluation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JudgeEvaluationRepository extends JpaRepository<JudgeEvaluation, Long> {

    Optional<JudgeEvaluation> findByJudgeIdAndApplicationId(Long judgeId, Long applicationId);

    @EntityGraph(attributePaths = "application")
    List<JudgeEvaluation> findByJudgeId(Long judgeId);
}
