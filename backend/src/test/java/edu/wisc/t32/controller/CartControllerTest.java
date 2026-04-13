package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.model.Cart;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.CartRepository;
import edu.wisc.t32.repository.ItemRepository;
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
class CartControllerTest {
  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private CartRepository cartRepository;

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private CurrentUserService currentUserService;

  @InjectMocks
  private CartController cartController;

  private User user(int id) {
    User u = new User();
    u.setUserId(id);
    return u;
  }

  private Cart cart(int userId, int itemId, int quantity) {
    Cart c = new Cart();
    c.setUserId(userId);
    c.setItemId(itemId);
    c.setQuantity(quantity);
    return c;
  }

  @Test
  void getCart_returnsEntries_whenAuthenticated() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user(1)));
    when(cartRepository.findByUserId(1)).thenReturn(List.of(cart(1, 10, 2), cart(1, 20, 1)));

    ResponseEntity<?> response = cartController.getCart(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals(2, ((List<?>) body.get("cart")).size());
  }

  @Test
  void getCart_returnsUnauthorized_whenNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = cartController.getCart(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals("Authentication required", ((Map<?, ?>) response.getBody()).get("error"));
  }

  @Test
  void addToCart_returnsCreated_whenValid() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user(1)));
    when(itemRepository.findByItemIdAndDeletedFalse(10)).thenReturn(Optional.of(new Item()));
    when(cartRepository.findByUserIdAndItemId(1, 10)).thenReturn(Optional.empty());
    when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

    ResponseEntity<?> response = cartController.addToCart(VALID_TOKEN, 10, 3);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Cart saved = (Cart) ((Map<?, ?>) response.getBody()).get("cart");
    assertEquals(1, saved.getUserId());
    assertEquals(10, saved.getItemId());
    assertEquals(3, saved.getQuantity());
    verify(cartRepository).save(any(Cart.class));
  }

  @Test
  void addToCart_returnsBadRequest_whenQuantityLessThanOne() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user(1)));

    ResponseEntity<?> response = cartController.addToCart(VALID_TOKEN, 10, 0);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Quantity must be at least 1", ((Map<?, ?>) response.getBody()).get("error"));
    verify(cartRepository, never()).save(any(Cart.class));
  }

  @Test
  void addToCart_returnsNotFound_whenItemDoesNotExist() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user(1)));
    when(itemRepository.findByItemIdAndDeletedFalse(99)).thenReturn(Optional.empty());

    ResponseEntity<?> response = cartController.addToCart(VALID_TOKEN, 99, 1);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Item not found", ((Map<?, ?>) response.getBody()).get("error"));
    verify(cartRepository, never()).save(any(Cart.class));
  }

  @Test
  void updateCartQuantity_returnsOk_whenValid() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user(1)));
    when(cartRepository.findByUserIdAndItemId(1, 10)).thenReturn(Optional.of(cart(1, 10, 1)));
    when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

    ResponseEntity<?> response = cartController.updateCartQuantity(VALID_TOKEN, 10, 5);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Cart saved = (Cart) ((Map<?, ?>) response.getBody()).get("cart");
    assertEquals(5, saved.getQuantity());
  }

  @Test
  void updateCartQuantity_returnsNotFound_whenItemNotInCart() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user(1)));
    when(cartRepository.findByUserIdAndItemId(1, 99)).thenReturn(Optional.empty());

    ResponseEntity<?> response = cartController.updateCartQuantity(VALID_TOKEN, 99, 2);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Item not in cart", ((Map<?, ?>) response.getBody()).get("error"));
    verify(cartRepository, never()).save(any(Cart.class));
  }

  @Test
  void removeFromCart_returnsOk_whenItemInCart() {
    Cart existing = cart(1, 10, 2);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user(1)));
    when(cartRepository.findByUserIdAndItemId(1, 10)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = cartController.removeFromCart(VALID_TOKEN, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals("Item removed from cart", body.get("message"));
    assertEquals(10, body.get("itemId"));
    verify(cartRepository).delete(existing);
  }

  @Test
  void removeFromCart_returnsNotFound_whenItemNotInCart() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user(1)));
    when(cartRepository.findByUserIdAndItemId(1, 99)).thenReturn(Optional.empty());

    ResponseEntity<?> response = cartController.removeFromCart(VALID_TOKEN, 99);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Item not in cart", ((Map<?, ?>) response.getBody()).get("error"));
    verify(cartRepository, never()).delete(any(Cart.class));
  }
}
