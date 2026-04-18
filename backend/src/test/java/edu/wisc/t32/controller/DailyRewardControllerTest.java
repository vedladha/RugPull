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
import org.mockito.ArgumentCaptor;
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
  private UserWallet mockWallet;

  @BeforeEach
  void setUp() {
    mockUser = new User();
    mockUser.setUserId(42);

    mockWallet = new UserWallet();
    mockWallet.setUserId(42);
  }

  // --- getDailyRewardStatus Tests ---

  @Test
  void getDailyRewardStatusReturnsFalseAtExactly24Hours() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    DailyReward reward = new DailyReward();
    reward.setStreak(5);
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(24));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));

    ResponseEntity<?> response = dailyRewardController.getDailyRewardStatus(JWT);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    DailyRewardStatusRequest status = extractStatus(response);
    assertFalse(status.isClaimed());
    assertEquals(5, status.getStreak());
  }

  @Test
  void getDailyRewardStatusReturnsTrueWhenUnder24Hours() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    DailyReward reward = new DailyReward();
    reward.setStreak(10);
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(23));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));

    ResponseEntity<?> response = dailyRewardController.getDailyRewardStatus(JWT);

    assertTrue(extractStatus(response).isClaimed());
    // Since streak is 10, next reward (streak 11) should be 12.0f
    assertEquals(12.0f, extractStatus(response).getNextReward());
  }

  // --- claimDailyReward Tests ---

  @Test
  void claimDailyRewardSuccessContinuesStreak() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));

    DailyReward reward = new DailyReward();
    reward.setUserId(42);
    reward.setStreak(9);
    // Claimed 25 hours ago, perfectly within the 48-hour grace period
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(25));

    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));
    when(userWalletRepository.findUserWalletByUserId(42)).thenReturn(Optional.of(mockWallet));

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Streak hits 10, so reward should scale up to 12.0f
    verify(walletService).fundAccount(eq(mockWallet), eq(12.0f));

    ArgumentCaptor<DailyReward> captor = ArgumentCaptor.forClass(DailyReward.class);
    verify(dailyRewardRepository).save(captor.capture());
    assertEquals(10, captor.getValue().getStreak());
  }

  @Test
  void claimDailyRewardSuccessResetsStreakWhenGracePeriodExceeded() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));

    DailyReward reward = new DailyReward();
    reward.setUserId(42);
    reward.setStreak(50); // Big streak
    // Claimed 50 hours ago (Exceeds 48 hour grace period)
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(50));

    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));
    when(userWalletRepository.findUserWalletByUserId(42)).thenReturn(Optional.of(mockWallet));

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Streak reset to 1, back to base reward of 10.0f
    verify(walletService).fundAccount(eq(mockWallet), eq(10.0f));

    ArgumentCaptor<DailyReward> captor = ArgumentCaptor.forClass(DailyReward.class);
    verify(dailyRewardRepository).save(captor.capture());
    assertEquals(1, captor.getValue().getStreak());
  }

  @Test
  void claimDailyRewardSuccessForNewUser() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.empty());
    when(userWalletRepository.findUserWalletByUserId(42)).thenReturn(Optional.of(mockWallet));

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(walletService).fundAccount(any(), eq(10.0f));

    ArgumentCaptor<DailyReward> captor = ArgumentCaptor.forClass(DailyReward.class);
    verify(dailyRewardRepository).save(captor.capture());
    assertEquals(1, captor.getValue().getStreak());
  }

  @Test
  void claimDailyRewardBadRequestWhenUnder24Hours() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));
    DailyReward reward = new DailyReward();
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(23));
    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(walletService, never()).fundAccount(any(), anyFloat());
  }

  @Test
  void claimDailyRewardInternalErrorWhenRpcFails() {
    when(currentUserService.getAuthenticatedUser(JWT)).thenReturn(Optional.of(mockUser));

    DailyReward reward = new DailyReward();
    reward.setStreak(1);
    reward.setClaimedLast(LocalDateTime.now(UTC).minusHours(25));

    when(dailyRewardRepository.findById(42)).thenReturn(Optional.of(reward));
    when(userWalletRepository.findUserWalletByUserId(42)).thenReturn(Optional.of(mockWallet));

    doThrow(new IllegalStateException()).when(walletService).fundAccount(any(), anyFloat());

    ResponseEntity<?> response = dailyRewardController.claimDailyReward(JWT);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    verify(dailyRewardRepository, never()).save(any());
  }

  @Test
  void claimDailyRewardInternalErrorWhenWalletNotFound() {
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
  void getDailyRewardStatusReturnsUnauthorizedWhenTokenIsMissingOrInvalid() {
    when(currentUserService.getAuthenticatedUser(any())).thenReturn(Optional.empty());
    ResponseEntity<?> response = dailyRewardController.getDailyRewardStatus(null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(dailyRewardRepository, never()).findById(anyInt());
  }

  @Test
  void claimDailyRewardReturnsUnauthorizedWhenTokenIsMissingOrInvalid() {
    when(currentUserService.getAuthenticatedUser(any())).thenReturn(Optional.empty());
    ResponseEntity<?> response = dailyRewardController.claimDailyReward("invalid-token");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(walletService, never()).fundAccount(any(), anyFloat());
    verify(dailyRewardRepository, never()).save(any());
  }

  private DailyRewardStatusRequest extractStatus(ResponseEntity<?> response) {
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    return (DailyRewardStatusRequest) body.get("status");
  }
}
