package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {
  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private WishlistRepository wishlistRepository;

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private CurrentUserService currentUserService;

  @InjectMocks
  private WishlistController wishlistController;

  // --- GET /wishlist ---

  @Test
  void getWishlist_returnsWishlistEntries_whenAuthenticated() {
    User user = new User();
    user.setUserId(1);
    Wishlist w1 = new Wishlist();
    w1.setUserId(1);
    w1.setItemId(10);
    Wishlist w2 = new Wishlist();
    w2.setUserId(1);
    w2.setItemId(20);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(user));
    when(wishlistRepository.findByUserId(1)).thenReturn(List.of(w1, w2));

    ResponseEntity<?> response = wishlistController.getWishlist(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    List<?> wishlist = (List<?>) body.get("wishlist");
    assertEquals(2, wishlist.size());
  }

  @Test
  void getWishlist_returnsEmptyList_whenNoItems() {
    User user = new User();
    user.setUserId(1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(user));
    when(wishlistRepository.findByUserId(1)).thenReturn(List.of());

    ResponseEntity<?> response = wishlistController.getWishlist(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    List<?> wishlist = (List<?>) body.get("wishlist");
    assertEquals(0, wishlist.size());
  }

  @Test
  void getWishlist_returnsUnauthorized_whenNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = wishlistController.getWishlist(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }

  // --- POST /wishlist/{itemId} ---

  @Test
  void addToWishlist_returnsCreated_whenValid() {
    User user = new User();
    user.setUserId(1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(user));
    when(itemRepository.findByItemIdAndDeletedFalse(10)).thenReturn(
        Optional.of(new Item()));
    when(wishlistRepository.existsById(new WishlistId(1, 10))).thenReturn(false);
    when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    ResponseEntity<?> response = wishlistController.addToWishlist(VALID_TOKEN, 10);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    Wishlist saved = (Wishlist) body.get("wishlist");
    assertEquals(1, saved.getUserId());
    assertEquals(10, saved.getItemId());
    verify(wishlistRepository).save(any(Wishlist.class));
  }

  @Test
  void addToWishlist_returnsUnauthorized_whenNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = wishlistController.addToWishlist(null, 10);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    verify(wishlistRepository, never()).save(any(Wishlist.class));
  }

  @Test
  void addToWishlist_returnsNotFound_whenItemDoesNotExist() {
    User user = new User();
    user.setUserId(1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(user));
    when(itemRepository.findByItemIdAndDeletedFalse(99)).thenReturn(Optional.empty());

    ResponseEntity<?> response = wishlistController.addToWishlist(VALID_TOKEN, 99);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not found", body.get("error"));
    verify(wishlistRepository, never()).save(any(Wishlist.class));
  }

  @Test
  void addToWishlist_returnsConflict_whenAlreadyInWishlist() {
    User user = new User();
    user.setUserId(1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(user));
    when(itemRepository.findByItemIdAndDeletedFalse(10)).thenReturn(
        Optional.of(new Item()));
    when(wishlistRepository.existsById(new WishlistId(1, 10))).thenReturn(true);

    ResponseEntity<?> response = wishlistController.addToWishlist(VALID_TOKEN, 10);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item already exists in wishlist", body.get("error"));
    verify(wishlistRepository, never()).save(any(Wishlist.class));
  }

  // --- DELETE /wishlist/{itemId} ---

  @Test
  void removeFromWishlist_returnsOk_whenItemInWishlist() {
    User user = new User();
    user.setUserId(1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(user));
    when(wishlistRepository.existsById(new WishlistId(1, 10))).thenReturn(true);

    ResponseEntity<?> response = wishlistController.removeFromWishlist(VALID_TOKEN, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item removed from wishlist", body.get("message"));
    assertEquals(10, body.get("itemId"));
    verify(wishlistRepository).deleteById(new WishlistId(1, 10));
  }

  @Test
  void removeFromWishlist_returnsUnauthorized_whenNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = wishlistController.removeFromWishlist(null, 10);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    verify(wishlistRepository, never()).deleteById(any());
  }

  @Test
  void removeFromWishlist_returnsNotFound_whenItemNotInWishlist() {
    User user = new User();
    user.setUserId(1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(user));
    when(wishlistRepository.existsById(new WishlistId(1, 99))).thenReturn(false);

    ResponseEntity<?> response = wishlistController.removeFromWishlist(VALID_TOKEN, 99);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not in wishlist", body.get("error"));
    verify(wishlistRepository, never()).deleteById(any());
  }

}
