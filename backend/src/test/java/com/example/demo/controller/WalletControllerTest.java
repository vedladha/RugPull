package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.model.User;
import com.example.demo.model.UserWallet;
import com.example.demo.repository.UserWalletRepository;
import com.example.demo.services.CurrentUserService;
import com.example.demo.services.RpcWalletService;
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
    UserWallet wallet = buildUserWallet(user);
    float expectedBalance = 250.75f;

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(userWalletRepository.findUserWalletByUser(user)).thenReturn(Optional.of(wallet));
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

    verify(userWalletRepository, never()).findUserWalletByUser(any());
    verify(walletService, never()).getWalletBalance(any());
  }

  // Checks that getWalletBalance returns 500 when the user has no associated wallet.
  @Test
  void getWalletBalance_returnsInternalServerError_whenWalletIsMissing() {
    User user = buildUser(2);

    when(currentUserService.getAuthenticatedUser(VALID_TOKEN)).thenReturn(Optional.of(user));
    when(userWalletRepository.findUserWalletByUser(user)).thenReturn(Optional.empty());

    ResponseEntity<?> response = walletController.getWalletBalance(VALID_TOKEN);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertEquals("Internal service error fetching wallet", body.get("error"));

    verify(walletService, never()).getWalletBalance(any());
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }

  private UserWallet buildUserWallet(User user) {
    UserWallet wallet = new UserWallet();
    wallet.setUser(user);
    wallet.setWalletAddress("test-wallet-address");
    return wallet;
  }
}
