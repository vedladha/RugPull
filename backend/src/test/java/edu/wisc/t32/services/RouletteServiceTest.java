package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.RouletteSpinResponse;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.UserWalletRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouletteServiceTest {
  @Mock
  private UserWalletRepository userWalletRepository;

  @Mock
  private RpcWalletService rpcWalletService;

  @Mock
  private Random random;

  @InjectMocks
  private RouletteService rouletteService;

  @Test
  void spinReturnsWinWhenRedBetMatches() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(wallet));
    when(rpcWalletService.getWalletBalance(wallet)).thenReturn(100.00f);
    when(random.nextInt(37)).thenReturn(1);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "color",
        "red"
    );

    assertEquals(Integer.valueOf(1), response.getWinningNumber());
    assertEquals("RED", response.getWinningColor());
    assertEquals("COLOR", response.getBetType());
    assertEquals("RED", response.getBetValue());
    assertEquals(new BigDecimal("10.00"), response.getWager());
    assertEquals(new BigDecimal("20.00"), response.getPayout());
    assertEquals(new BigDecimal("10.00"), response.getNetChange());
    assertEquals(new BigDecimal("110.00"), response.getBalance());
    assertTrue(response.isWon());
    verify(rpcWalletService).transferFromOperator(wallet, 10.00f);
    verify(rpcWalletService, never()).transferToOperator(any(), anyFloat());
  }

  @Test
  void spinReturnsLossWhenColorBetMisses() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(wallet));
    when(rpcWalletService.getWalletBalance(wallet)).thenReturn(100.00f);
    when(random.nextInt(37)).thenReturn(1);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "COLOR",
        "BLACK"
    );

    assertEquals(Integer.valueOf(1), response.getWinningNumber());
    assertEquals("RED", response.getWinningColor());
    assertEquals(new BigDecimal("0.00"), response.getPayout());
    assertEquals(new BigDecimal("-10.00"), response.getNetChange());
    assertEquals(new BigDecimal("90.00"), response.getBalance());
    assertFalse(response.isWon());
    verify(rpcWalletService).transferToOperator(wallet, 10.00f);
    verify(rpcWalletService, never()).transferFromOperator(any(), anyFloat());
  }

  @Test
  void spinTreatsZeroAsLossForColorBet() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(wallet));
    when(rpcWalletService.getWalletBalance(wallet)).thenReturn(100.00f);
    when(random.nextInt(37)).thenReturn(0);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "COLOR",
        "RED"
    );

    assertEquals(Integer.valueOf(0), response.getWinningNumber());
    assertEquals("GREEN", response.getWinningColor());
    assertEquals(new BigDecimal("0.00"), response.getPayout());
    assertEquals(new BigDecimal("-10.00"), response.getNetChange());
    assertFalse(response.isWon());
    verify(rpcWalletService).transferToOperator(wallet, 10.00f);
  }

  @Test
  void spinThrowsWhenBetTypeIsUnsupported() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("10.00"), "NUMBER", "17")
    );

    assertEquals("Unsupported bet type", error.getMessage());
    verify(userWalletRepository, never()).findUserWalletByUserId(7);
  }

  @Test
  void spinThrowsWhenBetValueIsInvalid() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("10.00"), "COLOR", "GREEN")
    );

    assertEquals("Bet value must be RED or BLACK", error.getMessage());
    verify(userWalletRepository, never()).findUserWalletByUserId(7);
  }

  @Test
  void spinThrowsWhenWagerIsInvalid() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("0.00"), "COLOR", "RED")
    );

    assertEquals("Wager must be greater than 0", error.getMessage());
    verify(userWalletRepository, never()).findUserWalletByUserId(7);
  }

  @Test
  void spinThrowsWhenWagerHasTooManyDecimals() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("1.001"), "COLOR", "RED")
    );

    assertEquals("Wager cannot have more than 2 decimal places", error.getMessage());
    verify(userWalletRepository, never()).findUserWalletByUserId(7);
  }

  @Test
  void spinThrowsWhenBalanceIsTooLow() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(wallet));
    when(rpcWalletService.getWalletBalance(wallet)).thenReturn(5.00f);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("10.00"), "COLOR", "RED")
    );

    assertEquals("Insufficient balance", error.getMessage());
    verify(rpcWalletService, never()).transferToOperator(wallet, 10.00f);
    verify(rpcWalletService, never()).transferFromOperator(any(), anyFloat());
  }

  private User buildUser(Integer userId) {
    User user = new User();
    user.setUserId(userId);
    return user;
  }

  private UserWallet buildWallet(Integer userId) {
    UserWallet wallet = new UserWallet();
    wallet.setUserId(userId);
    wallet.setWalletAddress("wallet-" + userId);
    wallet.setWalletPrivateKey("private-key");
    return wallet;
  }
}
