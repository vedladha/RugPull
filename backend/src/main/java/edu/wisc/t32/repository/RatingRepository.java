package edu.wisc.t32.repository;

import edu.wisc.t32.model.Rating;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for managing {@link Rating} entities.
 */
public interface RatingRepository extends JpaRepository<Rating, Integer> {

  /**
   * Finds a user's active rating for a specific item.
   *
   * @param userId the user who left the rating
   * @param itemId the item that was rated
   * @return the rating if it exists and is not deletedd
   */
  Optional<Rating> findByUserIdAndItemIdAndDeletedFalse(Integer userId, Integer itemId);

  /**
   * Retrieves all active ratings for a given item.
   *
   * @param itemId the item to retrieve ratings for
   * @return a list of non-deleted ratings for the item
   */
  List<Rating> findByItemIdAndDeletedFalse(Integer itemId);
}
