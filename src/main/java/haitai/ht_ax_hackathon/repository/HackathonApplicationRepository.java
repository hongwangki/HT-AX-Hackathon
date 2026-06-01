package haitai.ht_ax_hackathon.repository;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HackathonApplicationRepository extends JpaRepository<HackathonApplication, Long> {

    @EntityGraph(attributePaths = "members")
    List<HackathonApplication> findAllByOrderByCreatedAtDesc();
}
