package edu.wisc.t32.services;

import edu.wisc.t32.model.ItemImage;
import edu.wisc.t32.repository.ItemImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Resolves the authenticated user for requests that carry a JWT cookie.
 */
@Service
public class ItemImageService {

  private final ItemImageRepository itemImageRepository;
  private final FileService fileService;

  /**
   * Creates a new item image service.
   *
   * @param itemImageRepository the item image repository
   * @param fileService         the file service
   */
  public ItemImageService(ItemImageRepository itemImageRepository,
                          FileService fileService) {
    this.itemImageRepository = itemImageRepository;
    this.fileService = fileService;
  }

  /**
   * Processes a file upload and associates it with an item.
   */
  @Transactional
  public ItemImage addImageToItem(MultipartFile file, Integer itemId, Integer userId,
                                  Integer position) {
    fileService.validate(file);

    // Save to Docker volume
    String imageUrl = fileService.save(file);

    // Create database record
    ItemImage itemImage = new ItemImage();
    itemImage.setItemId(itemId);
    itemImage.setUserId(userId);
    itemImage.setImageUrl(imageUrl);
    itemImage.setAltText("Image for item " + itemId);
    itemImage.setPosition(position);

    return itemImageRepository.save(itemImage);
  }

  /**
   * Removes an image from the database and filesystem.
   */
  @Transactional
  public void deleteImage(Integer imageId) {
    ItemImage image = itemImageRepository.findById(imageId)
        .orElseThrow(() -> new RuntimeException("Image not found"));

    // Delete from database
    itemImageRepository.delete(image);

    // Delete from disk
    fileService.delete(image.getImageUrl());
  }
}
