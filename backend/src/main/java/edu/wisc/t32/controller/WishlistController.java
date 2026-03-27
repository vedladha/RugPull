package edu.wisc.t32.controller;

import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.Wishlist;
import edu.wisc.t32.model.WishlistId;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.WishlistRepository;
import edu.wisc.t32.services.CurrentUserService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the authenticated user's wishlist.
 */
@RestController
@RequestMapping("/wishlist")
public class WishlistController {

  private final WishlistRepository wishlistRepository;
  private final ItemRepository itemRepository;
  private final CurrentUserService currentUserService;

  /**
   * Constructs a WishlistController with the dependencies needed for wishlist lookups.
   *
   * @param wishlistRepository repository used for wishlist rows
   * @param itemRepository repository used to validate item existence
   * @param currentUserService service used to resolve the authenticated user
   */
  public WishlistController(
      WishlistRepository wishlistRepository,
      ItemRepository itemRepository,
      CurrentUserService currentUserService) {
    this.wishlistRepository = wishlistRepository;
    this.itemRepository = itemRepository;
    this.currentUserService = currentUserService;
  }

  /**
   * GETS all wishlist entries for the authenticated user.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @return a list of wishlist entries for the current user
   */
  @GetMapping
  public ResponseEntity<?> getWishlist(@CookieValue(name = "jwt", required = false) String token) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    List<Wishlist> wishlist = wishlistRepository.findByUserId(currentUser.get().getUserId());
    return ResponseEntity.ok(Map.of("wishlist", wishlist));
  }

  /**
   * POSTS an item to the authenticated user's wishlist.
   *
   * @param token  the JWT token extracted from the HTTP-only cookie
   * @param itemId the ID of the item to add
   * @return the created wishlist entry, or an error if the item doesn't exist or is
   *         already wishlisted
   */
  @PostMapping("/{itemId}")
  public ResponseEntity<?> addToWishlist(
      @CookieValue(name = "jwt", required = false) String token,
      @PathVariable Integer itemId) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    Optional<Item> item = itemRepository.findByItemIdAndDeletedFalse(itemId);
    if (item.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Item not found"));
    }

    Integer userId = currentUser.get().getUserId();
    WishlistId wishlistId = new WishlistId(userId, itemId);

    if (wishlistRepository.existsById(wishlistId)) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "Item already exists in wishlist"));
    }

    Wishlist wishlist = new Wishlist();
    wishlist.setUserId(userId);
    wishlist.setItemId(itemId);

    Wishlist saved = wishlistRepository.save(wishlist);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("wishlist", saved));
  }

  /**
   * DELETES an item from the authenticated user's wishlist.
   *
   * @param token  the JWT token extracted from the HTTP-only cookie
   * @param itemId the ID of the item to remove
   * @return a confirmation message, or 404 if the item was not in the wishlist
   */
  @DeleteMapping("/{itemId}")
  public ResponseEntity<?> removeFromWishlist(
      @CookieValue(name = "jwt", required = false) String token,
      @PathVariable Integer itemId) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    Integer userId = currentUser.get().getUserId();
    WishlistId wishlistId = new WishlistId(userId, itemId);

    if (!wishlistRepository.existsById(wishlistId)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Item not in wishlist"));
    }

    wishlistRepository.deleteById(wishlistId);
    return ResponseEntity.ok(Map.of("message", "Item removed from wishlist", "itemId", itemId));
  }
}

