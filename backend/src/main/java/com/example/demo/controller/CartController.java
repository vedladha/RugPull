package com.example.demo.controller;

import com.example.demo.model.Cart;
import com.example.demo.model.Item;
import com.example.demo.model.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ItemRepository;
import com.example.demo.services.CurrentUserService;
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
 * REST controller for managing the authenticated user's cart.
 */
@RestController
@RequestMapping("/cart")
public class CartController {

  private final CartRepository cartRepository;
  private final ItemRepository itemRepository;
  private final CurrentUserService currentUserService;

  /**
   * Constructs a CartController with the dependencies needed for cart operations.
   *
   * @param cartRepository    repository used for cart rows
   * @param itemRepository    repository used to validate item existence
   * @param currentUserService service used to resolve the authenticated user
   */
  public CartController(
      CartRepository cartRepository,
      ItemRepository itemRepository,
      CurrentUserService currentUserService) {
    this.cartRepository = cartRepository;
    this.itemRepository = itemRepository;
    this.currentUserService = currentUserService;
  }

  /**
   * GETS all cart entries for the authenticated user.
   *
   * @param token the JWT token extracted from the HTTP-only cookie
   * @return a list of cart entries for the current user
   */
  @GetMapping
  public ResponseEntity<?> getCart(@CookieValue(name = "jwt", required = false) String token) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    List<Cart> cart = cartRepository.findByUserId(currentUser.get().getUserId());
    return ResponseEntity.ok(Map.of("cart", cart));
  }

  /**
   * POSTS an item to the authenticated user's cart.
   *
   * @param token    the JWT token extracted from the HTTP-only cookie
   * @param itemId   the ID of the item to add
   * @param quantity the quantity to add (defaults to 1)
   * @return the created cart entry, or an error if the item doesn't exist or is already in the cart
   */
  @PostMapping("/{itemId}")
  public ResponseEntity<?> addToCart(
      @CookieValue(name = "jwt", required = false) String token,
      @PathVariable Integer itemId,
      @RequestParam(defaultValue = "1") Integer quantity) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    if (quantity < 1) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "Quantity must be at least 1"));
    }

    Optional<Item> item = itemRepository.findByItemIdAndDeletedFalse(itemId);
    if (item.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Item not found"));
    }

    Integer userId = currentUser.get().getUserId();
    Optional<Cart> existing = cartRepository.findByUserIdAndItemId(userId, itemId);

    if (existing.isPresent()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "Item already exists in cart"));
    }

    Cart cart = new Cart();
    cart.setUserId(userId);
    cart.setItemId(itemId);
    cart.setQuantity(quantity);

    Cart saved = cartRepository.save(cart);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("cart", saved));
  }

  /**
   * PUTS an updated quantity for an item in the authenticated user's cart.
   *
   * @param token    the JWT token extracted from the HTTP-only cookie
   * @param itemId   the ID of the item to update
   * @param quantity the new quantity
   * @return the updated cart entry, or 404 if the item is not in the cart
   */
  @PutMapping("/{itemId}")
  public ResponseEntity<?> updateCartQuantity(
      @CookieValue(name = "jwt", required = false) String token,
      @PathVariable Integer itemId,
      @RequestParam Integer quantity) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    if (quantity < 1) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "Quantity must be at least 1"));
    }

    Integer userId = currentUser.get().getUserId();
    Optional<Cart> existing = cartRepository.findByUserIdAndItemId(userId, itemId);

    if (existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Item not in cart"));
    }

    Cart cart = existing.get();
    cart.setQuantity(quantity);
    Cart saved = cartRepository.save(cart);
    return ResponseEntity.ok(Map.of("cart", saved));
  }

  /**
   * DELETES an item from the authenticated user's cart.
   *
   * @param token  the JWT token extracted from the HTTP-only cookie
   * @param itemId the ID of the item to remove
   * @return a confirmation message, or 404 if the item was not in the cart
   */
  @DeleteMapping("/{itemId}")
  public ResponseEntity<?> removeFromCart(
      @CookieValue(name = "jwt", required = false) String token,
      @PathVariable Integer itemId) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Authentication required"));
    }

    Integer userId = currentUser.get().getUserId();
    Optional<Cart> existing = cartRepository.findByUserIdAndItemId(userId, itemId);

    if (existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Item not in cart"));
    }

    cartRepository.delete(existing.get());
    return ResponseEntity.ok(Map.of("message", "Item removed from cart", "itemId", itemId));
  }
}
