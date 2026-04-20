package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.ItemBatchRequest;
import edu.wisc.t32.dto.ItemCreateRequest;
import edu.wisc.t32.dto.ItemModelDto;
import edu.wisc.t32.dto.ItemUpdateRequest;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.ItemImage;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.repository.ItemImageRepository;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.ItemImageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {
  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private ItemImageService itemImageService;

  @Mock
  private ItemImageRepository itemImageRepository;

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

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, request, null);

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

    ResponseEntity<?> response = itemController.createItem(null, request, null);

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

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, request, null);

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

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, request, null);

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

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, null, null);

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

    ResponseEntity<?> response = itemController.createItem(VALID_TOKEN, request, null);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    Item saved = (Item) body.get("item");
    assertNotNull(saved);
    assertEquals("Hello", saved.getName());
    assertEquals("World", saved.getDescription());
    verify(itemRepository).save(any(Item.class));
  }

  // Create an item with an associated image
  @Test
  void createItem_callsImageService_whenFileIsProvided() {
    ItemCreateRequest request = buildCreateRequest("Item", "Desc", "10", 1);
    MockMultipartFile mockFile =
        new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
    List<MultipartFile> fileList = List.of(mockFile);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
      Item item = inv.getArgument(0);
      item.setItemId(101);
      return item;
    });

    itemController.createItem(VALID_TOKEN, request, fileList);

    verify(itemImageService, times(1)).addImageToItem(eq(mockFile), eq(101), eq(7), eq(0));
  }

  // Create an item with multiple associated images
  @Test
  void createItem_callsImageServiceForEveryFileInList() {
    MockMultipartFile file1 =
        new MockMultipartFile("files", "front.jpg", "image/jpeg", "data1".getBytes());
    MockMultipartFile file2 =
        new MockMultipartFile("files", "back.jpg", "image/jpeg", "data2".getBytes());
    List<MultipartFile> fileList = List.of(file1, file2);

    ItemCreateRequest request = buildCreateRequest("Item", "Desc", "70", 1);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
      Item item = inv.getArgument(0);
      item.setItemId(500);
      return item;
    });

    itemController.createItem(VALID_TOKEN, request, fileList);

    // Verify the service was called twice (once per file)
    verify(itemImageService, times(2)).addImageToItem(any(), eq(500), eq(7), anyInt());
  }

  // Checks that getAllItems returns all active items with their thumbnails.
  @Test
  void getAllItems_returnsListOfActiveItems() {
    ItemImage image1 = new ItemImage();
    image1.setImageUrl("/test-image.jpg");

    UserProfile profile1 = mock(UserProfile.class);
    when(profile1.getDisplayName()).thenReturn("Seller One");

    UserProfile profile2 = mock(UserProfile.class);
    when(profile2.getDisplayName()).thenReturn("Seller Two");

    Item item1 = buildItem(1, 7, "Item One", "Description one", new BigDecimal("10.00"), 3, false);
    Item item2 = buildItem(2, 8, "Item Two", "Description two", new BigDecimal("20.00"), 5, false);

    when(itemRepository.findByDeletedFalse()).thenReturn(List.of(item1, item2));
    when(userProfileRepository.findByUserId(7)).thenReturn(profile1);
    when(userProfileRepository.findByUserId(8)).thenReturn(profile2);

    when(itemImageRepository.findByItemIdOrderByPositionAsc(1)).thenReturn(List.of(image1));
    when(itemImageRepository.findByItemIdOrderByPositionAsc(2)).thenReturn(List.of());

    ResponseEntity<?> response = itemController.getAllItems();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    List<?> items = (List<?>) body.get("items");
    assertEquals(2, items.size());

    // Verify the first item attached the image URL
    Map<?, ?> responseItem1 = (Map<?, ?>) items.get(0);
    assertEquals("/test-image.jpg", responseItem1.get("thumbnailUrl"));
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
    when(itemImageRepository.findByItemIdOrderByPositionAsc(1)).thenReturn(List.of());

    ResponseEntity<?> response = itemController.getItem(1);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    Item item = (Item) body.get("item");
    assertNotNull(item);
    assertNotNull(body.get("images"));
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

  // Checks that getItemImages returns the list of images when they exist
  @Test
  void getItemImages_returnsImages_whenImagesExist() {
    Integer itemId = 101;
    ItemImage img1 = new ItemImage();
    img1.setImageId(1);
    img1.setItemId(itemId);
    img1.setImageUrl("/images/thumb1.jpg");
    img1.setPosition(0);

    ItemImage img2 = new ItemImage();
    img2.setImageId(2);
    img2.setItemId(itemId);
    img2.setImageUrl("/images/thumb2.jpg");
    img2.setPosition(1);

    List<ItemImage> imageList = List.of(img1, img2);
    when(itemImageRepository.findByItemIdOrderByPositionAsc(itemId))
        .thenReturn(imageList);

    ResponseEntity<?> response = itemController.getItemImages(itemId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);

    List<?> resultList = (List<?>) body.get("images");
    assertEquals(2, resultList.size());

    ItemImage firstImage = (ItemImage) resultList.get(0);
    assertEquals("/images/thumb1.jpg", firstImage.getImageUrl());
    verify(itemImageRepository).findByItemIdOrderByPositionAsc(itemId);
  }

  // Checks that getItemImages returns 404 when no images are found for the ID
  @Test
  void getItemImages_returnsNotFound_whenNoImagesFound() {
    Integer itemId = 999;
    when(itemImageRepository.findByItemIdOrderByPositionAsc(itemId))
        .thenReturn(List.of());

    ResponseEntity<?> response = itemController.getItemImages(itemId);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not found", body.get("error"));
    verify(itemImageRepository).findByItemIdOrderByPositionAsc(itemId);
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

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 1, request, null);

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

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 99, request, null);

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

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 5, request, null);

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

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 6, request, null);

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

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 10, null, null);

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

    ResponseEntity<?> response = itemController.updateItem(null, 1, request, null);

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

    ResponseEntity<?> response = itemController.updateItem(VALID_TOKEN, 12, request, null);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("You do not own this item", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that a valid partial item patch saves new values and should return 200.
  @Test
  void patchItem_returnsUpdatedItem_whenPartialRequestIsValid() {
    // Only update price and stock, leave name and description null
    ItemUpdateRequest request = new ItemUpdateRequest();
    request.setPrice(new BigDecimal("49.95"));
    request.setStock(12);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    Item existing =
        buildItem(1, 7, "Old Name", "Old Description", new BigDecimal("1.00"), 1, false);
    when(itemRepository.findByItemIdAndDeletedFalse(1)).thenReturn(Optional.of(existing));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ResponseEntity<?> response = itemController.patchItem(VALID_TOKEN, 1, request, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    Map<?, ?> body = (Map<?, ?>) response.getBody();
    Item saved = (Item) body.get("item");

    assertNotNull(saved);
    assertEquals(1, saved.getItemId());
    assertEquals(7, saved.getUserId());
    assertEquals("Old Name", saved.getName()); // Should remain unchanged
    assertEquals("Old Description", saved.getDescription()); // Should remain unchanged
    assertEquals(0, new BigDecimal("49.95").compareTo(saved.getPrice())); // Should be updated
    assertEquals(12, saved.getStock()); // Should be updated

    verify(itemRepository).save(any(Item.class));
  }

  // Checks that patching with files saves the files using the service
  @Test
  void patchItem_callsImageService_whenFileIsProvided() {
    Item existing = buildItem(1, 7, "Name", "Desc", new BigDecimal("10"), 1, false);
    MockMultipartFile mockFile =
        new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.findByItemIdAndDeletedFalse(1)).thenReturn(Optional.of(existing));
    when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

    ItemUpdateRequest request = buildRequest("Name", "Desc", "10", 1);
    List<MultipartFile> fileList = List.of(mockFile);
    itemController.patchItem(VALID_TOKEN, 1, request, fileList);

    verify(itemImageService, times(1)).addImageToItem(eq(mockFile), eq(1), eq(7), eq(0));
  }

  // Checks that patching a missing item id is handled and should return 404.
  @Test
  void patchItem_returnsNotFound_whenItemDoesNotExist() {
    ItemUpdateRequest request = new ItemUpdateRequest();
    request.setPrice(new BigDecimal("49.95"));
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.findByItemIdAndDeletedFalse(99)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.patchItem(VALID_TOKEN, 99, request, null);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Item not found", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that invalid patch data is rejected and should return 400.
  @Test
  void patchItem_returnsBadRequest_whenValidationFails() {
    Item existing = buildItem(5, 8, "Name", "Description", new BigDecimal("2.00"), 3, false);

    // Provide a negative stock to trigger validation failure
    ItemUpdateRequest request = new ItemUpdateRequest();
    request.setStock(-5);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(8)));
    when(itemRepository.findByItemIdAndDeletedFalse(5)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = itemController.patchItem(VALID_TOKEN, 5, request, null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("stock must be non-negative", body.get("error"));
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that patching an item without auth returns 401.
  @Test
  void patchItem_returnsUnauthorized_whenUserIsNotAuthenticated() {
    ItemUpdateRequest request = new ItemUpdateRequest();
    request.setPrice(new BigDecimal("49.95"));
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.patchItem(null, 1, request, null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
    verify(itemRepository, never()).findByItemIdAndDeletedFalse(any());
    verify(itemRepository, never()).save(any(Item.class));
  }

  // Checks that patching another user's item returns 403.
  @Test
  void patchItem_returnsForbidden_whenUserDoesNotOwnItem() {
    Item existing = buildItem(12, 20, "Name", "Description", new BigDecimal("4.00"), 2, false);
    ItemUpdateRequest request = new ItemUpdateRequest();
    request.setPrice(new BigDecimal("49.95"));

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(21)));
    when(itemRepository.findByItemIdAndDeletedFalse(12)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = itemController.patchItem(VALID_TOKEN, 12, request, null);

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

  @Test
  void getItemsBatch_returnsItems_whenIdsAreValid() {
    List<Integer> ids = List.of(1, 2);

    Item item1 = buildItem(1, 7, "Item One", "Description", new BigDecimal("10.00"), 3, false);
    Item item2 = buildItem(2, 8, "Item Two", "Description", new BigDecimal("20.00"), 5, false);
    List<Item> mockItems = List.of(item1, item2);

    when(itemRepository.findByItemIdInAndDeletedFalse(ids)).thenReturn(mockItems);
    when(itemImageRepository.findByItemIdOrderByPositionAsc(1)).thenReturn(List.of());
    when(itemImageRepository.findByItemIdOrderByPositionAsc(2)).thenReturn(List.of());

    ResponseEntity<?> responseEntity = itemController.getItemsBatch(ids);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    ItemBatchRequest batchResponse =
        assertInstanceOf(ItemBatchRequest.class, responseEntity.getBody(),
            "response body should be an item batch request");
    assertNotNull(batchResponse);

    List<ItemModelDto> items = batchResponse.getItems();
    assertEquals(2, items.size());
    assertEquals(1, items.getFirst().getItemId());
    assertEquals("Item One", items.getFirst().getName());

    verify(itemRepository).findByItemIdInAndDeletedFalse(ids);
  }

  @Test
  void getItemsBatch_returnsItems_whenIdsAreInvalid() {
    List<Integer> ids = List.of(1, 2);
    when(itemRepository.findByItemIdInAndDeletedFalse(ids)).thenReturn(List.of());
    ResponseEntity<?> responseEntity = itemController.getItemsBatch(ids);
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) responseEntity.getBody();

    assertNotNull(body);
    assertEquals("No item's founding matching input list", body.get("error"));
    verify(itemRepository).findByItemIdInAndDeletedFalse(any());
  }

  @Test
  void getItemsBatch_returnsBadRequest_whenIdsListIsNull() {
    ResponseEntity<?> responseEntity = itemController.getItemsBatch(null);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) responseEntity.getBody();
    assertNotNull(body);
    assertEquals("request body is empty or missing", body.get("error"));

    verify(itemRepository, never()).findByItemIdInAndDeletedFalse(any());
  }

  @Test
  void getItemsBatch_returnsBadRequest_whenIdsListIsEmpty() {
    ResponseEntity<?> responseEntity = itemController.getItemsBatch(List.of());

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) responseEntity.getBody();
    assertNotNull(body);
    assertEquals("request body is empty or missing", body.get("error"));

    verify(itemRepository, never()).findByItemIdInAndDeletedFalse(any());
  }

  // Checks that getMyItems returns the user's items
  @Test
  void getMyItems_returnsItems_whenAuthenticatedAndHasItems() {
    Item item1 = buildItem(1, 7, "My Item One", "Description", new BigDecimal("10.00"), 3, false);
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.findByUserId(7)).thenReturn(List.of(item1));
    when(itemImageRepository.findByItemIdOrderByPositionAsc(1)).thenReturn(List.of());

    ResponseEntity<?> response = itemController.getMyItems(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    ItemBatchRequest batchResponse = assertInstanceOf(ItemBatchRequest.class, response.getBody());
    assertNotNull(batchResponse);
    assertEquals(1, batchResponse.getItems().size());
    assertEquals(1, batchResponse.getItems().getFirst().getItemId());
    assertEquals("My Item One", batchResponse.getItems().getFirst().getName());
  }

  // Checks that getMyItems returns 401 when unauthenticated
  @Test
  void getMyItems_returnsUnauthorized_whenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = itemController.getMyItems(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));
  }

  // Checks that getMyItems returns 404 when user has no items
  @Test
  void getMyItems_returnsNotFound_whenUserHasNoItems() {
    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(
        Optional.of(buildUser(7)));
    when(itemRepository.findByUserId(7)).thenReturn(List.of());

    ResponseEntity<?> response = itemController.getMyItems(VALID_TOKEN);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("No item's founding matching input list", body.get("error"));
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
