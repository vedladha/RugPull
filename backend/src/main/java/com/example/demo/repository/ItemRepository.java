package com.example.demo.repository;

import com.example.demo.model.Item;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Integer> {
    // Main lookup for item endpoints. Excludes the soft deleted rows.
    Optional<Item> findByItemIdAndDeletedFalse(Integer itemId);

    // Returns all active (non-deleted) items.
    List<Item> findByDeletedFalse();
}
