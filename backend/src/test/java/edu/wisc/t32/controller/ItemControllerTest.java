package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.ItemCreateRequest;
import edu.wisc.t32.dto.ItemUpdateRequest;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.services.CurrentUserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {
  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private CurrentUserService currentUserService;

  @Mock
  private UserProfileRepository userProfileRepository;

  @InjectMocks
  private ItemController itemController;

  // Checks that a create request was made
  @Test
  void createItem_returnsCreatedItem_whenRequestIsValid() {
    ItemCreateRequest request = buildCreateRequest("New Item", "A new item", "25", 3);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));

    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
      Item saved = invocation.getArgument(0);
      saved.setItemId(1);
      return saved;
    });

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    Item saved = (Item) body.get("item");
    assertNotNull(saved);
    assertEquals(1, saved.getItemId());
    assertEquals(7, saved.getUserId());
    assertEquals("New Item", saved.getName());
    assertEquals("A new item", saved.getDescription());
    assertEquals(0, new BigDecimal("25").compareTo(saved.getPrice()));
    assertEquals(3, saved.getStock());
    verify(itemRepository).save(any(Item.class));
  }

  // Checks that creating an item without auth returns 401.
  @Test
  void createItem_returnsUnauthorized_whenUserIsNotAuthenticated() {
    ItemCreateRequest request = buildCreateRequest("Name", "Description", "10.00", 1);
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.createItem(null, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that blank name is rejected and should return 400.
  @Test
  void createItem_returnsBadRequest_whenNameIsBlank() {
    ItemCreateRequest request = buildCreateRequest("   ", "Description", "10.00", 1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(1)));

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("name is required", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that negative price is rejected and returns 400 status
  @Test
  void createItem_returnsBadRequest_whenPriceIsNegative() {
    ItemCreateRequest request = buildCreateRequest("Name", "Description", "-1.00", 1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(1)));

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("price must be non-negative", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that a null request body is rejected
  @Test
  void createItem_returnsBadRequest_whenRequestBodyMissing() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(1)));

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertTrue(String.valueOf(body.get("error")).contains("required"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that name and description as typed matches the database naming conventions
  @Test
  void createItem_trimsStringFields_beforeSave() {
    ItemCreateRequest request = buildCreateRequest("  Hello  ", "  World  ", "1.00", 1);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(1)));

    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    Item saved = (Item) body.get("item");
    assertNotNull(saved);
    assertEquals("Hello", saved.getName());
    assertEquals("World", saved.getDescription());
    verify(itemRepository).save(any(Item.class));
  }

  // Checks that getAllItems returns all active items.
  @Test
  void getAllItems_returnsListOfActiveItems() {
    Item item1 = buildItem(1, 7, "Item One", "Description one", new BigDecimal("10.00"), 3, false);
    Item item2 = buildItem(2, 8, "Item Two", "Description two", new BigDecimal("20.00"), 5, false);

    UserProfile profile1 = mock(UserProfile.class);
    when(profile1.getDisplayName()).thenReturn("Seller One");

    UserProfile profile2 = mock(UserProfile.class);
    when(profile2.getDisplayName()).thenReturn("Seller Two");

    when(itemRepository.findByDeletedFalse()).thenReturn(List.of(item1, item2));
    when(userProfileRepository.findByUserId(7)).thenReturn(profile1);
    when(userProfileRepository.findByUserId(8)).thenReturn(profile2);

    ResponseEntity<?> response = itemController.getAllItems();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    List<?> items = (List<?>) body.get("items");
    assertEquals(2, items.size());
  }

  // Checks that getAllItems returns empty list when no items exist.
  @Test
  void getAllItems_returnsEmptyList_whenNoItemsExist() {
    when(itemRepository.findByDeletedFalse()).thenReturn(List.of());

    ResponseEntity<?> response = itemController.getAllItems();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    List<?> items = (List<?>) body.get("items");
    assertTrue(items.isEmpty());
  }

  // Checks that getItem returns the item when it exists.
  @Test
  void getItem_returnsItem_whenItemExists() {
    Item existing = buildItem(1, 7, "Name", "Description", new BigDecimal("10.00"), 3, false);
    when(itemRepository.findByItemIdAndDeletedFalse(1)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = itemController.getItem(1);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    Item item = (Item) body.get("item");
    assertNotNull(item);
    assertEquals(1, item.getItemId());
    assertEquals("Name", item.getName());
  }

  // Checks that getItem returns 404 when item does not exist.
  @Test
  void getItem_returnsNotFound_whenItemDoesNotExist() {
    when(itemRepository.findByItemIdAndDeletedFalse(5)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.getItem(5);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not found", body.get("error"));
  }

  // Checks that a valid item update saves new values and should return 200.
  @Test
  void updateItem_returnsUpdatedItem_whenRequestIsValid() {
    Item existing =
        buildItem(1, 7, "Old Name", "Old Description", new BigDecimal("1.00"), 1, false);
    ItemUpdateRequest request = buildRequest("Updated Item", "Updated description", "49.95", 12);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.findByItemIdAndDeletedFalse(1)).thenReturn(Optional.of(existing));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 1, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    Map<?, ?> body = (Map<?, ?>) response.getBody();
    Item saved = (Item) body.get("item");

    assertNotNull(saved);
    assertEquals(1, saved.getItemId());
    assertEquals(7, saved.getUserId());
    assertEquals("Updated Item", saved.getName());
    assertEquals("Updated description", saved.getDescription());
    assertEquals(0, new BigDecimal("49.95").compareTo(saved.getPrice()));
    assertEquals(12, saved.getStock());

    verify(itemRepository).save(any(Item.class));
  }

  // Checks that updating a missing item id is handled and should return 404.
  @Test
  void updateItem_returnsNotFound_whenItemDoesNotExist() {
    ItemUpdateRequest request = buildRequest("Updated Item", "Updated description", "49.95", 12);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.findByItemIdAndDeletedFalse(99)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 99, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not found", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that invalid update data is rejected and should return 400.
  @Test
  void updateItem_returnsBadRequest_whenValidationFails() {
    Item existing = buildItem(5, 8, "Name", "Description", new BigDecimal("2.00"), 3, false);
    ItemUpdateRequest request = buildRequest("   ", "Updated description", "-1", -2);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(8)));
    when(itemRepository.findByItemIdAndDeletedFalse(5)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 5, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("name is required", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that name and description are trimmed before save and should return 200.
  @Test
  void updateItem_trimsStringFields_beforeSave() {
    Item existing = buildItem(6, 9, "Name", "Description", new BigDecimal("10.00"), 2, false);
    ItemUpdateRequest request = buildRequest("  Trim Me  ", "  Keep Tight  ", "1.00", 1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(9)));
    when(itemRepository.findByItemIdAndDeletedFalse(6)).thenReturn(Optional.of(existing));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 6, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    Item saved = (Item) body.get("item");
    assertNotNull(saved);
    assertEquals("Trim Me", saved.getName());
    assertEquals("Keep Tight", saved.getDescription());
    verify(itemRepository).save(any(Item.class));
  }

  // Checks that a null request body is rejected and should return 400.
  @Test
  void updateItem_returnsBadRequest_whenRequestBodyMissing() {
    Item existing = buildItem(10, 4, "Name", "Description", new BigDecimal("3.50"), 1, false);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(4)));
    when(itemRepository.findByItemIdAndDeletedFalse(10)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 10, null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertTrue(String.valueOf(body.get("error")).contains("required"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that updating an item without auth returns 401.
  @Test
  void updateItem_returnsUnauthorized_whenUserIsNotAuthenticated() {
    ItemUpdateRequest request = buildRequest("Updated Item", "Updated description", "49.95", 12);
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.updateItem(null, 1, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    verify(itemRepository, never()).findByItemIdAndDeletedFalse(any());
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that updating another user's item returns 403.
  @Test
  void updateItem_returnsForbidden_whenUserDoesNotOwnItem() {
    Item existing = buildItem(12, 20, "Name", "Description", new BigDecimal("4.00"), 2, false);
    ItemUpdateRequest request = buildRequest("Updated Item", "Updated description", "49.95", 12);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(21)));
    when(itemRepository.findByItemIdAndDeletedFalse(12)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 12, request);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("You do not own this item", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that deleting an existing item marks it deleted and should return 200.
  @Test
  void deleteItem_returnsOk_andMarksDeleted_whenItemExists() {
    Item existing = buildItem(11, 2, "Name", "Description", new BigDecimal("2.00"), 5, false);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(2)));
    when(itemRepository.findByItemIdAndDeletedFalse(11)).thenReturn(Optional.of(existing));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ResponseEntity<?> response = itemController.deleteItem(VALID_TOKEN, 11);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item deleted", body.get("message"));
    assertEquals(11, body.get("itemId"));

    ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
    verify(itemRepository).save(itemCaptor.capture());
    assertTrue(Boolean.TRUE.equals(itemCaptor.getValue().getDeleted()));
  }

  // Checks that deleting a missing item id is handled and should return 404.
  @Test
  void deleteItem_returnsNotFound_whenItemDoesNotExist() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.findByItemIdAndDeletedFalse(404)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.deleteItem(VALID_TOKEN, 404);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not found", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that deleting an item without auth returns 401.
  @Test
  void deleteItem_returnsUnauthorized_whenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.deleteItem(null, 1);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    verify(itemRepository, never()).findByItemIdAndDeletedFalse(any());
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that deleting another user's item returns 403.
  @Test
  void deleteItem_returnsForbidden_whenUserDoesNotOwnItem() {
    Item existing = buildItem(13, 30, "Name", "Description", new BigDecimal("5.00"), 2, false);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(31)));
    when(itemRepository.findByItemIdAndDeletedFalse(13)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = itemController.deleteItem(VALID_TOKEN, 13);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("You do not own this item", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  private ItemCreateRequest buildCreateRequest(String name, String description, String price,
                                               Integer stock) {
    ItemCreateRequest request = new ItemCreateRequest();
    request.setName(name);
    request.setDescription(description);
    request.setPrice(new BigDecimal(price));
    request.setStock(stock);
    return request;
  }

  private ItemUpdateRequest buildRequest(String name, String description, String price,
                                         Integer stock) {
    ItemUpdateRequest request = new ItemUpdateRequest();
    request.setName(name);
    request.setDescription(description);
    request.setPrice(new BigDecimal(price));
    request.setStock(stock);
    return request;
  }

  private Item buildItem(Integer itemId, Integer userId, String name, String description,
                         BigDecimal price, Integer stock, Boolean deleted) {
    Item item = new Item();
    item.setItemId(itemId);
    item.setUserId(userId);
    item.setName(name);
    item.setDescription(description);
    item.setPrice(price);
    item.setStock(stock);
    item.setDeleted(deleted);
    return item;
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }
}
