package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.Rating;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.OrderItemRepository;
import edu.wisc.t32.repository.RatingRepository;
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

/**
 * Unit tests for {@link RatingController}.
 */
@ExtendWith(MockitoExtension.class)
class RatingControllerTest {

  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private RatingRepository ratingRepository;

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private OrderItemRepository orderItemRepository;

  @Mock
  private CurrentUserService currentUserService;

  @InjectMocks
  private RatingController ratingController;

  @Test
  void createRating_returnsUnauthorized_whenNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = ratingController.createRating(null, 10, 5);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    verify(ratingRepository, never()).save(any(Rating.class));
  }

  @Test
  void createRating_returnsBadRequest_whenValueIsNull() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));

    ResponseEntity<?> response = ratingController.createRating(VALID_TOKEN, 10, null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("ratingValue is required", body.get("error"));
    verify(ratingRepository, never()).save(any(Rating.class));
  }

  @Test
  void createRating_returnsBadRequest_whenValueTooLow() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));

    ResponseEntity<?> response = ratingController.createRating(VALID_TOKEN, 10, 0);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("ratingValue must be between 1 and 5", body.get("error"));
  }

  @Test
  void createRating_returnsBadRequest_whenValueTooHigh() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));

    ResponseEntity<?> response = ratingController.createRating(VALID_TOKEN, 10, 6);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("ratingValue must be between 1 and 5", body.get("error"));
  }

  @Test
  void createRating_returnsNotFound_whenItemDoesNotExist() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(itemRepository.findByItemIdAndDeletedFalse(99))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = ratingController.createRating(VALID_TOKEN, 99, 5);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not found", body.get("error"));
    verify(ratingRepository, never()).save(any(Rating.class));
  }

  @Test
  void createRating_returnsForbidden_whenUserHasNotPurchased() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(itemRepository.findByItemIdAndDeletedFalse(10))
        .thenReturn(Optional.of(new Item()));
    when(orderItemRepository.existsByUserIdAndItemId(1, 10))
        .thenReturn(false);

    ResponseEntity<?> response = ratingController.createRating(VALID_TOKEN, 10, 5);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("You can only rate items you have purchased", body.get("error"));
    verify(ratingRepository, never()).save(any(Rating.class));
  }

  @Test
  void createRating_returnsConflict_whenAlreadyRated() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(itemRepository.findByItemIdAndDeletedFalse(10))
        .thenReturn(Optional.of(new Item()));
    when(orderItemRepository.existsByUserIdAndItemId(1, 10))
        .thenReturn(true);
    when(ratingRepository.findByUserIdAndItemIdAndDeletedFalse(1, 10))
        .thenReturn(Optional.of(new Rating()));

    ResponseEntity<?> response = ratingController.createRating(VALID_TOKEN, 10, 5);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("You have already rated this item", body.get("error"));
    verify(ratingRepository, never()).save(any(Rating.class));
  }

  @Test
  void updateRating_returnsUnauthorized_whenNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = ratingController.updateRating(null, 10, 5);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(ratingRepository, never()).save(any(Rating.class));
  }

  @Test
  void updateRating_returnsBadRequest_whenValueOutOfRange() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));

    ResponseEntity<?> response = ratingController.updateRating(VALID_TOKEN, 10, 0);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("ratingValue must be between 1 and 5", body.get("error"));
  }

  @Test
  void updateRating_returnsNotFound_whenNoExistingRating() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(ratingRepository.findByUserIdAndItemIdAndDeletedFalse(1, 10))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = ratingController.updateRating(VALID_TOKEN, 10, 4);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Rating not found", body.get("error"));
    verify(ratingRepository, never()).save(any(Rating.class));
  }


  @Test
  void getItemRatings_returnsZeros_whenNoRatings() {
    when(ratingRepository.findByItemIdAndDeletedFalse(10))
        .thenReturn(List.of());

    ResponseEntity<?> response = ratingController.getItemRatings(10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals(0, body.get("total"));
    assertEquals(0.0, body.get("average"));

    Map<?, ?> distribution = (Map<?, ?>) body.get("distribution");
    for (int star = 1; star <= 5; star++) {
      assertEquals(0L, distribution.get(star));
    }
  }


  @Test
  void getUserRating_returnsNullRating_whenNotExists() {
    User user = new User();
    user.setUserId(1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(ratingRepository.findByUserIdAndItemIdAndDeletedFalse(1, 10))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = ratingController.getUserRating(VALID_TOKEN, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertNull(body.get("rating"));
  }

  @Test
  void getUserRating_returnsUnauthorized_whenNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = ratingController.getUserRating(null, 10);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }


  @Test
  void deleteRating_returnsUnauthorized_whenNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = ratingController.deleteRating(null, 10);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(ratingRepository, never()).save(any(Rating.class));
    }

  @Test
  void deleteRating_returnsNotFound_whenNoRating() {
    User user = new User();
    user.setUserId(1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN))
        .thenReturn(Optional.of(user));
    when(ratingRepository.findByUserIdAndItemIdAndDeletedFalse(1, 10))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = ratingController.deleteRating(VALID_TOKEN, 10);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Rating not found", body.get("error"));
    verify(ratingRepository, never()).save(any(Rating.class));
    }
}

