package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileServiceTest {

  private FileService fileService;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    fileService = new FileService();
    // Manually inject the temp directory path into the private field
    ReflectionTestUtils.setField(fileService, "uploadPath", tempDir.toString());
    // Manually trigger the @PostConstruct logic
    fileService.init();
  }

  @Test
  void whenFileIsTooLarge_thenThrowException() {
    // Create a 6MB file (Limit is 5MB)
    byte[] content = new byte[6 * 1024 * 1024];
    MockMultipartFile largeFile = new MockMultipartFile(
        "file", "too_big.jpg", "image/jpeg", content
    );

    RuntimeException ex = assertThrows(RuntimeException.class, () -> {
      fileService.validate(largeFile);
    });

    assertTrue(ex.getMessage().contains("too large"));
  }

  @Test
  void whenValidFile_thenSaveSuccessfully() {
    MockMultipartFile validFile = new MockMultipartFile(
        "file", "test.png", "image/png", "fake-image-content".getBytes()
    );

    String resultPath = fileService.save(validFile);

    assertTrue(resultPath.startsWith("/images/"));
    assertTrue(resultPath.endsWith(".png"));

    // Verify the file exists in the temp directory
    String filename = resultPath.substring(resultPath.lastIndexOf("/") + 1);
    assertTrue(tempDir.resolve(filename).toFile().exists());
  }

  @Test
  void whenFileIsInvalidImage_thenThrowException() {
    // Plain text labeled as an image
    MockMultipartFile fakeImage = new MockMultipartFile(
        "file", "malicious.jpg", "image/jpeg", "not-an-image-content".getBytes()
    );

    assertThrows(RuntimeException.class, () -> fileService.validate(fakeImage));
  }
}
