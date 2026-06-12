package haitai.ht_ax_hackathon.repository;

import haitai.ht_ax_hackathon.domain.TaskSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

    Optional<TaskSubmission> findByApplicationId(Long applicationId);

    @EntityGraph(attributePaths = "files")
    List<TaskSubmission> findWithFilesByApplicationIdIn(List<Long> applicationIds);
}
