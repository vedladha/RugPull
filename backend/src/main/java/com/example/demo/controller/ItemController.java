package com.example.demo.controller;

import com.example.demo.dto.ItemCreateRequest;
import com.example.demo.dto.ItemUpdateRequest;
import com.example.demo.model.Item;
import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.services.CurrentUserService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing item entities.
 * Provides endpoints for creating, retrieving, updating, and soft-deleting items.
 */
@RestController
@RequestMapping("/items")
public class ItemController {

  private final ItemRepository itemRepository;
  private final CurrentUserService currentUserService;
  private final UserProfileRepository userProfileRepository;

  /**
   * Constructs an ItemController with the necessary repository dependency.
   *
   * @param itemRepository          the repository used for item database operations
   * @param currentUserService      service used to resolve the authenticated user
   * @param userProfileRepository   the reposiroty used for user profile operations
   */
  public ItemController(ItemRepository itemRepository, 
                        CurrentUserService currentUserService, 
                        UserProfileRepository userProfileRepository) {
    this.itemRepository = itemRepository;
    this.currentUserService = currentUserService;
    this.userProfileRepository = userProfileRepository;
  }

  /**
   * Creates a new item listing.
   * Requires all fields to be provided in the request body.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @param request the data transfer object containing the new item details
   * @return a {@link ResponseEntity} with status 201 (CREATED) containing the saved item,
   *        or a 400 (BAD REQUEST) with an error message if validation fails
   */
  @PostMapping
  public ResponseEntity<?> createItem(@CookieValue(name = "jwt", required = false) String token,
                                      @RequestBody ItemCreateRequest request) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
          "Authentication required"));
    }

    String validationError = validateCreate(request);
    if (validationError != null) {
      return ResponseEntity.badRequest().body(Map.of("error", validationError));
    }

    Item item = new Item();
    item.setUserId(currentUser.get().getUserId());
    item.setName(request.getName().trim());
    item.setDescription(request.getDescription().trim());
    item.setPrice(request.getPrice());
    item.setStock(request.getStock());

    Item saved = itemRepository.save(item);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("item",
        saved));
  }

  /**
   * Retrieves all active (non-deleted) items from the database.
   *
   * @return a {@link ResponseEntity} containing a list of all active items
   */
  @GetMapping
  public ResponseEntity<?> getAllItems() {
    List<Item> items = itemRepository.findByDeletedFalse();
    List<Map<String, Object>> response = items.stream()
        .map(item -> {
          Map<String, Object> map = new HashMap<>();
          map.put("itemId", item.getItemId());
          map.put("name", item.getName());
          map.put("description", item.getDescription());
          map.put("price", item.getPrice());
          map.put("stock", item.getStock());
          UserProfile seller = userProfileRepository.findByUserId(item.getUserId());
          map.put("sellerName", seller.getDisplayName());
          return map;
        })
        .collect(Collectors.toList());
    return ResponseEntity.ok(Map.of("items", response));
  }

  /**
   * Retrieves a single active item by its ID.
   *
   * @param itemId the unique identifier of the item to retrieve
   * @return a {@link ResponseEntity} containing the item, or a 404 NOT FOUND if the item
   *         does not exist or is marked as deleted
   */
  @GetMapping("/{itemId}")
  public ResponseEntity<?> getItem(@PathVariable Integer itemId) {
    Optional<Item> existing = itemRepository.findByItemIdAndDeletedFalse(itemId);
    if (existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",
          "Item not found"));
    }
    return ResponseEntity.ok(Map.of("item", existing.get()));
  }

  /**
   * Performs a full update for an existing, active item.
   * Requires all fields to be present in the request body.
   *
   * @param itemId  the unique identifier of the item to update
   * @param request the data transfer object containing the updated item details
   * @return a {@link ResponseEntity} containing the updated item, a 400 BAD REQUEST
   *         if validation fails,  or a 404 NOT FOUND if the item does not exist
   */
  @PutMapping("/{itemId}")
  public ResponseEntity<?> updateItem(@CookieValue(name = "jwt", required = false) String token,
                                      @PathVariable Integer itemId,
                                      @RequestBody ItemUpdateRequest request) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
          "Authentication required"));
    }

    Optional<Item> existing = itemRepository.findByItemIdAndDeletedFalse(itemId);
    if (existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",
          "Item not found"));
    }

    Item item = existing.get();
    if (!item.getUserId().equals(currentUser.get().getUserId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error",
          "You do not own this item"));
    }

    String validationError = validate(request);
    if (validationError != null) {
      return ResponseEntity.badRequest().body(Map.of("error", validationError));
    }

    item.setName(request.getName().trim());
    item.setDescription(request.getDescription().trim());
    item.setPrice(request.getPrice());
    item.setStock(request.getStock());

    Item saved = itemRepository.save(item);
    return ResponseEntity.ok(Map.of("item", saved));
  }

  /**
   * Performs a soft delete on an item.
   * Keeps the database row intact but marks the item as deleted so it is filtered out by standard
   * repository lookups.
   *
   * @param itemId the unique identifier of the item to delete
   * @return a {@link ResponseEntity} confirming the deletion, or a 404 NOT FOUND if the item does
   *        not exist
   */
  @DeleteMapping("/{itemId}")
  public ResponseEntity<?> deleteItem(@CookieValue(name = "jwt", required = false) String token,
                                      @PathVariable Integer itemId) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
          "Authentication required"));
    }

    Optional<Item> existing = itemRepository.findByItemIdAndDeletedFalse(itemId);
    if (existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",
          "Item not found"));
    }

    Item item = existing.get();
    if (!item.getUserId().equals(currentUser.get().getUserId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error",
          "You do not own this item"));
    }

    item.setDeleted(true);
    itemRepository.save(item);
    return ResponseEntity.ok(Map.of("message", "Item deleted", "itemId", itemId));
  }

  /**
   * Validates the fields of an incoming {@link ItemCreateRequest}.
   *
   * @param request the creation request payload to validate
   * @return a string containing the validation error message, or {@code null} if all fields are
   *         valid
   */
  private String validateCreate(ItemCreateRequest request) {
    if (request == null) {
      return "Request body is required";
    }
    if (isBlank(request.getName())) {
      return "name is required";
    }
    if (isBlank(request.getDescription())) {
      return "description is required";
    }
    if (request.getPrice() == null) {
      return "price is required";
    }
    if (request.getStock() == null) {
      return "stock is required";
    }
    if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
      return "price must be non-negative";
    }
    if (request.getStock() < 0) {
      return "stock must be non-negative";
    }
    return null;
  }

  /**
   * Validates the fields of an incoming {@link ItemUpdateRequest}.
   *
   * @param request the update request payload to validate
   * @return a string containing the validation error message, or {@code null} if all fields are
   *         valid
   */
  private String validate(ItemUpdateRequest request) {
    if (request == null) {
      return "Request body is required";
    }
    if (isBlank(request.getName())) {
      return "name is required";
    }
    if (isBlank(request.getDescription())) {
      return "description is required";
    }
    if (request.getPrice() == null) {
      return "price is required";
    }
    if (request.getStock() == null) {
      return "stock is required";
    }
    if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
      return "price must be non-negative";
    }
    if (request.getStock() < 0) {
      return "stock must be non-negative";
    }
    return null;
  }

  /**
   * Utility method to check if a string is null, empty, or contains only whitespace.
   *
   * @param value the string to check
   * @return {@code true} if the string is null or blank; {@code false} otherwise
   */
  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
