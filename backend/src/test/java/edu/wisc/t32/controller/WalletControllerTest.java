package edu.wisc.t32.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.WalletFundRequest;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.UserWalletRepository;
import edu.wisc.t32.services.CurrentUserService;
import edu.wisc.t32.services.RpcWalletService;
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
class WalletControllerTest {

  private static final String VALID_TOKEN = "valid-token";

  @Mock
  private RpcWalletService walletService;

  @Mock
  private CurrentUserService currentUserService;

  @Mock
  private UserWalletRepository userWalletRepository;

  @InjectMocks
  private WalletController walletController;

  // Checks that getWalletBalance returns the balance when auth and wallet are valid.
  @Test
  void getWalletBalance_returnsBalance_whenUserAndWalletExist() {
    User user = buildUser(1);
    UserWallet wallet = buildUserWallet(user.getUserId());
    float expectedBalance = 250.75f;

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(userWalletRepository.findUserWalletByUserId(user.getUserId()))
        .thenReturn(Optional.of(wallet));
    when(walletService.getWalletBalance(wallet)).thenReturn(expectedBalance);

    ResponseEntity<?> response = walletController.getWalletBalance(VALID_TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedBalance, response.getBody());
    verify(walletService).getWalletBalance(wallet);
  }

  // Checks that getWalletBalance returns 401 when the user is not authenticated.
  @Test
  void getWalletBalance_returnsUnauthorized_whenUserIsNotAuthenticated() {
    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = walletController.getWalletBalance(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));

    verify(userWalletRepository, never()).findUserWalletByUserId(any());
    verify(walletService, never()).getWalletBalance(any());
  }

  // Checks that getWalletBalance returns 500 when the user has no associated wallet.
  @Test
  void getWalletBalance_returnsInternalServerError_whenWalletIsMissing() {
    User user = buildUser(2);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(userWalletRepository.findUserWalletByUserId(user.getUserId()))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = walletController.getWalletBalance(VALID_TOKEN);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Internal service error fetching wallet", body.get("error"));

    verify(walletService, never()).getWalletBalance(any());
  }

  // --- fundWallet Tests ---

  @Test
  void fundWallet_returnsUnauthorized_whenUserIsNotAuthenticated() {
    WalletFundRequest request = new WalletFundRequest();
    request.setAmount(100.0f);

    when(currentUserService.getAuthenticatedUser(null)).thenReturn(Optional.empty());

    ResponseEntity<?> response = walletController.fundWallet(null, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Authentication required", body.get("error"));

    verify(walletService, never()).fundAccount(any(), anyFloat());
  }

  @Test
  void fundWallet_returnsInternalServerError_whenWalletIsMissing() {
    User user = buildUser(1);
    WalletFundRequest request = new WalletFundRequest();
    request.setAmount(100.0f);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(userWalletRepository.findUserWalletByUserId(user.getUserId()))
        .thenReturn(Optional.empty());

    ResponseEntity<?> response = walletController.fundWallet(VALID_TOKEN, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Internal service error fetching wallet", body.get("error"));

    verify(walletService, never()).fundAccount(any(), anyFloat());
  }

  @Test
  void fundWallet_returnsBadRequest_whenAmountIsZeroOrLess() {
    User user = buildUser(1);
    UserWallet wallet = buildUserWallet(user.getUserId());
    WalletFundRequest request = new WalletFundRequest();
    request.setAmount(-50.0f); // Works for 0.0f as well

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(userWalletRepository.findUserWalletByUserId(user.getUserId()))
        .thenReturn(Optional.of(wallet));

    ResponseEntity<?> response = walletController.fundWallet(VALID_TOKEN, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Can not fund account with 0 or less tokens", body.get("error"));

    verify(walletService, never()).fundAccount(any(), anyFloat());
  }

  @Test
  void fundWallet_returnsInternalServerError_whenHederaNetworkFails() {
    User user = buildUser(1);
    UserWallet wallet = buildUserWallet(user.getUserId());
    WalletFundRequest request = new WalletFundRequest();
    request.setAmount(100.0f);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(userWalletRepository.findUserWalletByUserId(user.getUserId()))
        .thenReturn(Optional.of(wallet));

    // Mock the IllegalStateException from Hedera
    doThrow(new IllegalStateException("Network unreachable"))
        .when(walletService).fundAccount(wallet, 100.0f);

    ResponseEntity<?> response = walletController.fundWallet(VALID_TOKEN, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Internal service error while funding wallet", body.get("error"));
  }

  @Test
  void fundWallet_returnsOk_whenFundingIsSuccessful() {
    User user = buildUser(1);
    UserWallet wallet = buildUserWallet(user.getUserId());
    WalletFundRequest request = new WalletFundRequest();
    request.setAmount(250.0f);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(userWalletRepository.findUserWalletByUserId(user.getUserId()))
        .thenReturn(Optional.of(wallet));

    ResponseEntity<?> response = walletController.fundWallet(VALID_TOKEN, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(walletService).fundAccount(wallet, 250.0f);
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }

  private UserWallet buildUserWallet(Integer userId) {
    UserWallet wallet = new UserWallet();
    wallet.setUserId(userId);
    wallet.setWalletAddress("test-wallet-address");
    return wallet;
  }
}
