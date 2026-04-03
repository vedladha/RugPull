package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import edu.wisc.t32.exception.WalletProvisioningException;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.UserProfileRepository;
import edu.wisc.t32.repository.UserRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Integration tests for {@link AuthService} registration atomicity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

  @Autowired
  private AuthService authService;

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private UserProfileRepository userProfileRepo;

  @Autowired
  private UserWalletRepository userWalletRepo;

  @MockitoBean
  private RpcWalletService walletService;

  @TestConfiguration
  static class TestCorsConfiguration {
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration configuration = new CorsConfiguration();
      configuration.addAllowedOrigin("http://localhost:3000");
      configuration.addAllowedHeader("*");
      configuration.addAllowedMethod("*");
      configuration.setAllowCredentials(true);

      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", configuration);
      return source;
    }
  }

  @BeforeEach
  void clearDatabase() {
    userWalletRepo.deleteAll();
    userProfileRepo.deleteAll();
    userRepo.deleteAll();
  }

  @Test
  void registerWithWallet_persistsUserProfileAndWallet_whenWalletCreationSucceeds() {
    when(walletService.createWallet()).thenReturn(
        new RpcWalletService.WalletCredentials("wallet-1", "private-key"));

    User registeredUser = authService.registerWithWallet("testuser", "test@example.com",
        "password");

    assertNotNull(registeredUser.getUserId());
    assertEquals(1, userRepo.count());
    assertEquals(1, userProfileRepo.count());
    assertEquals(1, userWalletRepo.count());

    User savedUser = userRepo.findByEmail("test@example.com").orElseThrow();
    UserProfile savedProfile = userProfileRepo.findByDisplayName("testuser").orElseThrow();
    UserWallet savedWallet = userWalletRepo.findById(savedUser.getUserId()).orElseThrow();

    assertEquals(savedUser.getUserId(), savedProfile.getUserId());
    assertEquals(savedUser.getUserId(), savedWallet.getUserId());
    assertEquals("wallet-1", savedWallet.getWalletAddress());
    assertEquals("private-key", savedWallet.getWalletPrivateKey());
  }

  @Test
  void registerWithWallet_rollsBackUserProfileAndWallet_whenWalletCreationFails() {
    when(walletService.createWallet()).thenThrow(new IllegalStateException("wallet failed"));

    WalletProvisioningException error = assertThrows(WalletProvisioningException.class,
        () -> authService.registerWithWallet("testuser", "test@example.com", "password"));

    assertEquals("wallet failed", error.getMessage());
    assertEquals(0, userRepo.count());
    assertEquals(0, userProfileRepo.count());
    assertEquals(0, userWalletRepo.count());
    assertTrue(userRepo.findByEmail("test@example.com").isEmpty());
    assertTrue(userProfileRepo.findByDisplayName("testuser").isEmpty());
  }
}
