package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.DailyRewardStatusRequest;
import edu.wisc.t32.model.DailyReward;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.DailyRewardRepository;
import edu.wisc.t32.repository.UserWalletRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.RpcWalletService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class DailyRewardControllerTest {

  private static final String JWT = "mock-token";
  private static final ZoneId UTC = ZoneId.of("UTC");

  @Mock private CurrentUserService currentUserService;
  @Mock private UserWalletRepository userWalletRepository;
  @Mock private DailyRewardRepository dailyRewardRepository;
  @Mock private RpcWalletService walletService;

  @InjectMocks private DailyRewardController dailyRewardController;

  private User mockUser;

  @BeforeEach
  void setUp() {
    mockUser = new User();
    mockUser.setUserId(42);
  }

  // --- getDailyRewardStatus Tests ---

  @Test
  void getDailyRewardStatus_ReturnsFalse_AtExactly24Hours() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    DailyReward reward = new DailyReward();
    // Exactly 24 hours satisfies >= 24
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(24));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));

    ResponseEntity<?> response = dailyRewardController.getDailyRewardStatus(JWT);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertFalse(extractStatus(response).isClaimed());
  }

  @Test
  void getDailyRewardStatus_ReturnsTrue_WhenUnder24Hours() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    DailyReward reward = new DailyReward();
    // 23 hours fails >= 24
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(23));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));

    ResponseEntity<?> response = dailyRewardController.getDailyRewardStatus(JWT);

    assertTrue(extractStatus(response).isClaimed());
  }

  // --- claimDailyReward Tests ---

  @Test
  void claimDailyReward_Success_AtExactly24Hours() {
    UserWallet wallet = new UserWallet();
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));

    DailyReward reward = new DailyReward();
    reward.setUserId(42);
    // 24 hours elapsed: (24 < 24) is FALSE, so check passes
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(24));

    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));
    when(userWalletRepository.findUserWalletByUserId(42)).thenReturn(Optional.of(wallet));

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(walletService).fundAccount(eq(wallet), eq(10.0f));
    verify(dailyRewardRepository).save(any());
  }

  @Test
  void claimDailyReward_Success_ForNewUser() {
    // New users are initialized with now.minusDays(1) (24 hours ago)
    // Since 24 is not < 24, this should now succeed.
    UserWallet wallet = new UserWallet();
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.empty());
    when(userWalletRepository.findUserWalletByUserId(42)).thenReturn(Optional.of(wallet));

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(walletService).fundAccount(any(), eq(10.0f));
    verify(dailyRewardRepository).save(any());
  }

  @Test
  void claimDailyReward_BadRequest_WhenUnder24Hours() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    DailyReward reward = new DailyReward();
    // 23 hours elapsed: (23 < 24) is TRUE, triggers BAD_REQUEST
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(23));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(walletService, never()).fundAccount(any(), anyFloat());
  }

  @Test
  void claimDailyReward_InternalError_WhenRpcFails() {
    UserWallet wallet = new UserWallet();
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));

    DailyReward reward = new DailyReward();
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(25));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));
    when(userWalletRepository.findUserWalletByUserId(42)).thenReturn(Optional.of(wallet));

    doThrow(new IllegalStateException()).when(walletService).fundAccount(any(), anyFloat());

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    verify(dailyRewardRepository, never()).save(any());
  }

  @Test
  void claimDailyReward_InternalError_WhenWalletNotFound() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    DailyReward reward = new DailyReward();
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(25));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));

    when(userWalletRepository.findUserWalletByUserId(42)).thenReturn(Optional.empty());

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  // --- Authentication Return Tests ---

  @Test
  void getDailyRewardStatus_ReturnsUnauthorized_WhenTokenIsMissingOrInvalid() {
    // Arrange: Simulate CurrentUserService returning an empty Optional
    when(currentUserService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

    // Act
    ResponseEntity<?> response = dailyRewardController.getDailyRewardStatus(null);

    // Assert
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));

    // Guard Rail: Ensure the database is NEVER queried if auth fails
    verify(dailyRewardRepository, never()).findById(anyInt());
  }

  @Test
  void claimDailyReward_ReturnsUnauthorized_WhenTokenIsMissingOrInvalid() {
    // Arrange: Simulate auth failure
    when(currentUserService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

    // Act
    ResponseEntity<?> response = dailyRewardController.claimDailyReward("invalid-token");

    // Assert
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));

    // Guard Rail: Ensure no financial transactions or DB updates are attempted
    verify(walletService, never()).fundAccount(any(), anyFloat());
    verify(dailyRewardRepository, never()).save(any());
  }

  private DailyRewardStatusRequest extractStatus(ResponseEntity<?> response) {
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    return (DailyRewardStatusRequest) body.get("status");
  }
}
