package edu.wisc.t32.repository;

import edu.wisc.t32.model.ItemImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for managing {@link ItemImage} entities.
 */
public interface ItemImageRepository extends JpaRepository<ItemImage, Integer> {

  /**
   * Finds the image associated with the id.
   *
   * @param imageId the ID of the image.
   * @return an Optional containing the image if it exists.
   */
  Optional<ItemImage> findByImageId(Integer imageId);

  /**
   * Finds all images associated with a specific item.
   *
   * @param itemId the ID of the item.
   * @return a list of images ordered by their position.
   */
  List<ItemImage> findByItemIdOrderByPositionAsc(Integer itemId);

  /**
   * Finds the primary image (position 0) for an item.
   *
   * @param itemId the ID of the item.
   * @return an Optional containing the primary image if it exists.
   */
  Optional<ItemImage> findFirstByItemIdOrderByPositionAsc(Integer itemId);

  /**
   * Deletes all images for a specific item.
   *
   * @param itemId the ID of the item.
   */
  void deleteByItemId(Integer itemId);
}
