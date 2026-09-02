package haitai.ht_ax_hackathon.repository;

import haitai.ht_ax_hackathon.domain.Judge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JudgeRepository extends JpaRepository<Judge, Long> {

    Optional<Judge> findByUsername(String username);

    List<Judge> findByActiveTrueOrderByNameAscUsernameAsc();

}
