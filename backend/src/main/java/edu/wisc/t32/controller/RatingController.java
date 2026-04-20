package edu.wisc.t32.controller;

import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Rating;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderItemRepository;
import edu.wisc.t32.repository.RatingRepository;
import edu.wisc.t32.services.CurrentUserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing item ratings on a 1-5 scale.
 * Users may only rate items they have purchased, and only one rating per user per item is allowed
 */
@RestController
@RequestMapping("/ratings")
public class RatingController {

  private final RatingRepository ratingRepository;
  private final ItemRepository itemRepository;
  private final OrderItemRepository orderItemRepository;
  private final CurrentUserService currentUserService;

  /**
   * Constructs a RatingController with the required repositories and services.
   */
  public RatingController(RatingRepository ratingRepository,
                          ItemRepository itemRepository,
                          OrderItemRepository orderItemRepository,
                          CurrentUserService currentUserService) {
    this.ratingRepository = ratingRepository;
    this.itemRepository = itemRepository;
    this.orderItemRepository = orderItemRepository;
    this.currentUserService = currentUserService;
  }

  /**
   * Creates a new rating for an item by the authenticated user.
   */
  @PostMapping("/{itemId}")
  public ResponseEntity<?> createRating(@CookieValue(name = "jwt", required = false) String token,
                                        @PathVariable Integer itemId,
                                        @RequestParam("value") Integer value) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    String validationError = validateRatingValue(value);
    if (validationError != null) {
      return ResponseEntity.badRequest().body(Map.of("error", validationError));
    }

    Optional<Item> item = itemRepository.findByItemIdAndDeletedFalse(itemId);
    if (item.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Item not found"));
    }

    Integer userId = currentUser.get().getUserId();

    if (!orderItemRepository.existsByUserIdAndItemId(userId, itemId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "You can only rate items you have purchased"));
    }

    if (ratingRepository.findByUserIdAndItemIdAndDeletedFalse(userId, itemId).isPresent()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "You have already rated this item"));
    }

    Rating rating = new Rating();
    rating.setUserId(userId);
    rating.setItemId(itemId);
    rating.setRatingValue(value);

    Rating saved = ratingRepository.save(rating);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("rating", saved));
  }

  /**
   * Updates the authenticated user's existing rating for an item.
   */
  @PutMapping("/{itemId}")
  public ResponseEntity<?> updateRating(@CookieValue(name = "jwt", required = false) String token,
                                        @PathVariable Integer itemId,
                                        @RequestParam("value") Integer value) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    String validationError = validateRatingValue(value);
    if (validationError != null) {
      return ResponseEntity.badRequest().body(Map.of("error", validationError));
    }

    Integer userId = currentUser.get().getUserId();
    Optional<Rating> existing = ratingRepository
        .findByUserIdAndItemIdAndDeletedFalse(userId, itemId);
    if (existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Rating not found"));
    }

    Rating rating = existing.get();
    rating.setRatingValue(value);
    Rating saved = ratingRepository.save(rating);
    return ResponseEntity.ok(Map.of("rating", saved));
  }

  /**
   * Retrieves aggregated rating information for an item: average score,
   * total count, and a distribution of counts per star value (1-5).
   */
  @GetMapping("/item/{itemId}")
  public ResponseEntity<?> getItemRatings(@PathVariable Integer itemId) {
    List<Rating> ratings = ratingRepository.findByItemIdAndDeletedFalse(itemId);

    Map<Integer, Long> distribution = new HashMap<>();
    for (int star = 1; star <= 5; star++) {
      distribution.put(star, 0L);
    }

    double sum = 0.0;
    for (Rating rating : ratings) {
      int value = rating.getRatingValue();
      distribution.merge(value, 1L, Long::sum);
      sum += value;
    }

    int total = ratings.size();
    double average = total == 0 ? 0.0 : sum / total;

    Map<String, Object> payload = new HashMap<>();
    payload.put("itemId", itemId);
    payload.put("average", average);
    payload.put("total", total);
    payload.put("distribution", distribution);
    return ResponseEntity.ok(payload);
  }

  /**
   * Retrieves the authenticated user's rating for a specific item, if it exists.
   *
   */
  @GetMapping("/user/{itemId}")
  public ResponseEntity<?> getUserRating(@CookieValue(name = "jwt", required = false) String token,
                                          @PathVariable Integer itemId) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    Integer userId = currentUser.get().getUserId();
    Optional<Rating> rating = ratingRepository
        .findByUserIdAndItemIdAndDeletedFalse(userId, itemId);

    Map<String, Object> payload = new HashMap<>();
    payload.put("rating", rating.orElse(null));
    return ResponseEntity.ok(payload);
  }

  /**
   * Deletes the authenticated user's rating for an item.
   */
  @DeleteMapping("/{itemId}")
  public ResponseEntity<?> deleteRating(@CookieValue(name = "jwt", required = false) String token,
                                        @PathVariable Integer itemId) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    Integer userId = currentUser.get().getUserId();
    Optional<Rating> existing = ratingRepository
        .findByUserIdAndItemIdAndDeletedFalse(userId, itemId);
    if (existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Rating not found"));
    }

    // instead of marking deleted the desing works better to actually explicitly remove
    // fixes front end bug where we try to post on a deleted review
    ratingRepository.delete(existing.get());
    return ResponseEntity.ok(Map.of("message", "Rating removed", "itemId", itemId));
  }

  /**
   * Validates that a rating value is present and on the 1-5 scale.
   */
  private String validateRatingValue(Integer value) {
    if (value == null) {
      return "ratingValue is required";
    }
    if (value < 1 || value > 5) {
      return "ratingValue must be between 1 and 5";
    }
    return null;
  }
}
