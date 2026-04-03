package edu.wisc.t32.repository;

import edu.wisc.t32.model.Item;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for managing {@link Item} entities.
 * Provides standard CRUD operations and custom query methods to handle soft-deleted records.
 */
public interface ItemRepository extends JpaRepository<Item, Integer> {

  /**
   * Main lookup method for item endpoints.
   * Retrieves an item by its ID while explicitly excluding any rows marked as soft-deleted.
   *
   * @param itemId the unique identifier of the item to search for
   * @return an {@link Optional} containing the item if it exists and is not deleted, or an empty
   *         Optional otherwise
   */
  Optional<Item> findByItemIdAndDeletedFalse(Integer itemId);

  /**
   * Retrieves and locks an active item row for purchase processing.
   *
   * <p>This method applies a pessimistic write lock so concurrent purchase requests for the same
   * item cannot both pass the stock check before one transaction commits.
   *
   * @param itemId the unique identifier of the item to lock
   * @return an {@link Optional} containing the locked item when it exists and is not deleted
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT item FROM Item item WHERE item.itemId = :itemId AND item.deleted = false")
  Optional<Item> findByItemIdAndDeletedFalseForUpdate(@Param("itemId") Integer itemId);

  /**
   * Retrieves all active items from the database.
   * Filters out any records that have been marked as soft-deleted.
   *
   * @return a {@link List} of all non-deleted items
   */
  List<Item> findByDeletedFalse();
}
