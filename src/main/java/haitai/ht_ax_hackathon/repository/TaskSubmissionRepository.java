package haitai.ht_ax_hackathon.repository;

import haitai.ht_ax_hackathon.domain.TaskSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

    Optional<TaskSubmission> findByApplicationId(Long applicationId);

    @EntityGraph(attributePaths = "files")
    List<TaskSubmission> findWithFilesByApplicationIdIn(List<Long> applicationIds);

    @EntityGraph(attributePaths = {"application", "files"})
    List<TaskSubmission> findAllByOrderByUpdatedAtDesc();

    /** Judge targets are limited to applications imported from the numbered guide spreadsheet. */
    @EntityGraph(attributePaths = {"application", "files"})
    @Query("""
            select submission
            from TaskSubmission submission
            where exists (
                select overview.applicationId
                from EvaluationGuideOverview overview
                where overview.applicationId = submission.application.id
                  and overview.sourceTotalScore is not null
            )
              and 6 = (
                select count(item.id)
                from EvaluationGuideItem item
                where item.application.id = submission.application.id
                  and item.weightedScore is not null
            )
            order by submission.updatedAt desc
            """)
    List<TaskSubmission> findJudgeEvaluationTargets();

    @EntityGraph(attributePaths = {"application", "files"})
    @Query("select submission from TaskSubmission submission where submission.application.id = :applicationId")
    Optional<TaskSubmission> findDetailByApplicationId(Long applicationId);
}
