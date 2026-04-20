package edu.wisc.t32.controller;

import edu.wisc.t32.dto.ItemBatchRequest;
import edu.wisc.t32.dto.ItemCreateRequest;
import edu.wisc.t32.dto.ItemModelDto;
import edu.wisc.t32.dto.ItemUpdateRequest;
import edu.wisc.t32.model.Item;
import edu.wisc.t32.model.ItemImage;
import edu.wisc.t32.model.User;
import edu.wisc.t32.repository.ItemImageRepository;
import edu.wisc.t32.repository.ItemRepository;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.FileService;
import edu.wisc.t32.services.ItemImageService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing item entities.
 * Provides endpoints for creating, retrieving, updating, patching, and soft-deleting items.
 */
@RestController
@RequestMapping("/items")
public class ItemController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemController.class);

  private final ItemRepository itemRepository;
  private final CurrentUserService currentUserService;
  private final UserProfileRepository userProfileRepository;
  private final FileService fileService;
  private final ItemImageService itemImageService;
  private final ItemImageRepository itemImageRepository;

  /**
   * Constructs an ItemController with the necessary repository dependency.
   *
   * @param itemRepository        the repository used for item database operations
   * @param currentUserService    service used to resolve the authenticated user
   * @param userProfileRepository the reposiroty used for user profile operations
   */
  public ItemController(ItemRepository itemRepository,
                        CurrentUserService currentUserService,
                        UserProfileRepository userProfileRepository,
                        FileService fileService,
                        ItemImageService itemImageService,
                        ItemImageRepository itemImageRepository) {
    this.itemRepository = itemRepository;
    this.currentUserService = currentUserService;
    this.userProfileRepository = userProfileRepository;
    this.fileService = fileService;
    this.itemImageService = itemImageService;
    this.itemImageRepository = itemImageRepository;
  }

  /**
   * Creates a new item listing.
   * Requires all fields to be provided in the request body.
   *
   * @param token   the JWT token extracted from the HTTP-only cookie
   * @param request the data transfer object containing the new item details
   * @return a {@link ResponseEntity} with status 201 (CREATED) containing the saved item,
   *        or a 400 (BAD REQUEST) with an error message if validation fails
   */
  @PostMapping
  @Transactional
  public ResponseEntity<?> createItem(@CookieValue(name = "jwt", required = false) String token,
                                      @RequestPart("item") ItemCreateRequest request,
                                      @RequestPart(value = "file", required = false)
                                      List<MultipartFile> files) {
    // Authentication
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
          "Authentication required"));
    }

    String validationError = validateCreate(request);
    if (validationError != null) {
      return ResponseEntity.badRequest().body(Map.of("error", validationError));
    }

    // Create item
    Item item = new Item();
    item.setUserId(currentUser.get().getUserId());
    item.setName(request.getName().trim());
    item.setDescription(request.getDescription().trim());
    item.setPrice(request.getPrice());
    item.setStock(request.getStock());
    Item savedItem = itemRepository.save(item);

    // Create item image if there is one
    if (files != null && !files.isEmpty()) {
      for (int i = 0; i < files.size(); i++) {
        itemImageService.addImageToItem(files.get(i), savedItem.getItemId(),
            currentUser.get().getUserId(), i);
      }
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("item",
        savedItem));
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
          map.put("sellerName",
              userProfileRepository.findByUserId(item.getUserId()).getDisplayName());

          List<ItemImage> images =
              itemImageRepository.findByItemIdOrderByPositionAsc(item.getItemId());
          if (!images.isEmpty()) {
            map.put("thumbnailUrl", images.get(0).getImageUrl());
            map.put("thumbnailUpdatedAt", images.get(0).getUpdatedAt());
          } else {
            map.put("thumbnailUrl", null);
          }
          return map;
        })
        .collect(Collectors.toList());
    return ResponseEntity.ok(Map.of("items", response));
  }

  /**
   * Retrieves a list of items specified in the body of this request returning a response.
   *
   * @param ids the list of ids finding in this batch request
   * @return a http response
   */
  @PostMapping("/batch")
  public ResponseEntity<?> getItemsBatch(@RequestBody List<Integer> ids) {
    final ItemBatchRequest response = ItemBatchRequest.next();
    if (ids == null || ids.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "request body is empty or missing"));
    }

    final List<Item> items = itemRepository.findByItemIdInAndDeletedFalse(ids);
    if (items.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "No item's founding matching input list"));
    }

    response.setItems(items.stream().map((item) -> {
      List<ItemImage> images = itemImageRepository.findByItemIdOrderByPositionAsc(item.getItemId());
      ItemImage image = !images.isEmpty() ? images.getFirst() : null;
      return ItemModelDto.fromItem(item, image);
    }).toList());
    return ResponseEntity.ok(response);
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
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Item not found"));
    }

    Item item = existing.get();

    // Fetch all images for this specific item
    List<ItemImage> images = itemImageRepository.findByItemIdOrderByPositionAsc(itemId);

    Map<String, Object> response = new HashMap<>();
    response.put("item", item);
    response.put("images", images);

    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves an ItemImage list of images associated with the itemId.
   *
   * @param itemId item to find the images for
   * @return a {@link ResponseEntity} containing a list of ItemImages, or a 404 NOT FOUND
   *         if there are no images
   */
  @GetMapping("/{itemId}/images")
  public ResponseEntity<?> getItemImages(@PathVariable Integer itemId) {
    List<ItemImage> images = itemImageRepository.findByItemIdOrderByPositionAsc(itemId);
    if (images.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",
          "Item not found"));
    }
    return ResponseEntity.ok(Map.of("images", images));
  }

  /**
   * Performs a full update for an existing, active item.
   * Requires all fields to be present in the request body.
   *
   * @param itemId  the unique identifier of the item to update
   * @param request the data transfer object containing the updated item details
   * @return a {@link ResponseEntity} containing the updated item, a 400 BAD REQUEST
   *         if validation fails, or a 404 NOT FOUND if the item does not exist
   */
  @PutMapping("/{itemId}")
  @Transactional
  public ResponseEntity<?> updateItem(@CookieValue(name = "jwt", required = false) String token,
                                      @PathVariable Integer itemId,
                                      @RequestPart("item") ItemUpdateRequest request,
                                      @RequestPart(value = "file", required = false)
                                      List<MultipartFile> files) {
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

    // IMAGES
    // Get existing images
    List<ItemImage> imgs = itemImageRepository.findByItemIdOrderByPositionAsc(itemId);
    int newFilesCount = (files != null) ? files.size() : 0;

    // Update/add images
    if (files != null && !files.isEmpty()) {
      for (int i = 0; i < files.size(); i++) {
        if (i < imgs.size()) { // Image already exists, overwrite it
          fileService.overwrite(imgs.get(i).getImageUrl(), files.get(i));
          imgs.get(i).setUpdatedAt(java.time.LocalDateTime.now());
          itemImageRepository.save(imgs.get(i));
        } else {
          itemImageService.addImageToItem(files.get(i), item.getItemId(),
              currentUser.get().getUserId(), i);
        }
      }
    }

    Item saved = itemRepository.save(item);
    return ResponseEntity.ok(Map.of("item", saved));
  }

  /**
   * Performs a partial update for an existing, active item.
   * Only the fields provided in the request body will be modified.
   *
   * @param itemId  the unique identifier of the item to update
   * @param request the data transfer object containing the fields to update
   * @return a {@link ResponseEntity} containing the updated item, a 400 BAD REQUEST
   *         if validation fails, or a 404 NOT FOUND if the item does not exist
   */
  @PatchMapping("/{itemId}")
  public ResponseEntity<?> patchItem(@CookieValue(name = "jwt", required = false) String token,
                                     @PathVariable Integer itemId,
                                     @RequestPart(value = "item", required = false)
                                     ItemUpdateRequest request,
                                     @RequestPart(value = "file", required = false)
                                     List<MultipartFile> files) {
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

    String validationError = validatePatch(request);
    if (validationError != null) {
      return ResponseEntity.badRequest().body(Map.of("error", validationError));
    }

    if (request.getName() != null) {
      item.setName(request.getName().trim());
    }
    if (request.getDescription() != null) {
      item.setDescription(request.getDescription().trim());
    }
    if (request.getPrice() != null) {
      item.setPrice(request.getPrice());
    }
    if (request.getStock() != null) {
      item.setStock(request.getStock());
    }

    Item saved = itemRepository.save(item);
    if (files != null && !files.isEmpty()) {
      for (int i = 0; i < files.size(); i++) {
        itemImageService.addImageToItem(files.get(i), saved.getItemId(),
            currentUser.get().getUserId(), i);
      }
    }

    return ResponseEntity.ok(Map.of("item", saved));
  }

  /**
   * Performs a soft delete on an item.
   * Keeps the database row intact but marks the item as deleted so it is filtered out by standard
   * repository lookups.
   *
   * @param itemId the unique identifier of the item to delete
   * @return a {@link ResponseEntity} confirming the deletion, or a 404 NOT FOUND if the item does
   *         not exist
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
   * Gets all items posted by the authenticated user.
   *
   * @param token the token of the authenticated user
   * @return a response
   */
  @GetMapping("/me")
  public ResponseEntity<?> getMyItems(@CookieValue(name = "jwt", required = false) String token) {
    Optional<User> currentUser = currentUserService.getAuthenticatedUser(token);
    if (currentUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
          "Authentication required"));
    }

    int id = currentUser.get().getUserId();
    List<Item> items = itemRepository.findByUserId(id);

    // re-use batch request
    if (items.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "No item's founding matching input list"));
    }

    ItemBatchRequest response = ItemBatchRequest.next();
    response.setItems(items.stream().map((item) -> {
      List<ItemImage> images = itemImageRepository.findByItemIdOrderByPositionAsc(item.getItemId());
      ItemImage image = !images.isEmpty() ? images.getFirst() : null;
      return ItemModelDto.fromItem(item, image);
    }).toList());
    return ResponseEntity.ok(response);
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
   * Validates the fields of an incoming {@link ItemUpdateRequest} for PUT operations.
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
   * Validates the fields of an incoming {@link ItemUpdateRequest} for PATCH operations.
   * Allows null fields since it's a partial update.
   *
   * @param request the update request payload to validate
   * @return a string containing the validation error message, or {@code null} if fields are valid
   */
  private String validatePatch(ItemUpdateRequest request) {
    if (request == null) {
      return "Request body is required";
    }
    if (request.getName() != null && isBlank(request.getName())) {
      return "name cannot be blank";
    }
    if (request.getDescription() != null && isBlank(request.getDescription())) {
      return "description cannot be blank";
    }
    if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
      return "price must be non-negative";
    }
    if (request.getStock() != null && request.getStock() < 0) {
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
