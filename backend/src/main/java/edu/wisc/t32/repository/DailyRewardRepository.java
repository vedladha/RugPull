package edu.wisc.t32.repository;

import edu.wisc.t32.model.DailyReward;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for managing {@link DailyReward} entities.
 */
public interface DailyRewardRepository extends JpaRepository<DailyReward, Integer> {
}
