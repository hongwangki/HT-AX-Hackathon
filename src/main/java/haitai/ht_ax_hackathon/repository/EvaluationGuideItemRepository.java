package haitai.ht_ax_hackathon.repository;

import haitai.ht_ax_hackathon.domain.EvaluationGuideItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationGuideItemRepository extends JpaRepository<EvaluationGuideItem, Long> {

    List<EvaluationGuideItem> findByApplicationIdOrderByDisplayOrderAsc(Long applicationId);

    Optional<EvaluationGuideItem> findByApplicationIdAndCriterionCode(
            Long applicationId, String criterionCode
    );

    long countByApplicationIdAndWeightedScoreIsNotNull(Long applicationId);

    void deleteByApplicationId(Long applicationId);
}
