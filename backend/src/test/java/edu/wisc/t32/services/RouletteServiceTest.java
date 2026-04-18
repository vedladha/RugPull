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

    stubWalletBalance(wallet, 7, 100.00f);
    when(random.nextInt(37)).thenReturn(1);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "COLOR",
        "RED"
    );

    assertEquals(Integer.valueOf(1), response.getWinningNumber());
    assertEquals("RED", response.getWinningColor());
    assertEquals("COLOR", response.getBetType());
    assertEquals("RED", response.getBetValue());
    assertEquals(new BigDecimal("20.00"), response.getPayout());
    assertEquals(new BigDecimal("10.00"), response.getNetChange());
    assertEquals(new BigDecimal("110.00"), response.getBalance());
    assertTrue(response.isWon());
    verify(rpcWalletService).transferFromOperator(wallet, 10.00f);
    verify(rpcWalletService, never()).transferToOperator(any(), anyFloat());
  }

  @Test
  void spinReturnsWinWhenParityBetMatches() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    stubWalletBalance(wallet, 7, 100.00f);
    when(random.nextInt(37)).thenReturn(24);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "PARITY",
        "EVEN"
    );

    assertEquals(Integer.valueOf(24), response.getWinningNumber());
    assertEquals("BLACK", response.getWinningColor());
    assertEquals("PARITY", response.getBetType());
    assertEquals("EVEN", response.getBetValue());
    assertEquals(new BigDecimal("20.00"), response.getPayout());
    assertEquals(new BigDecimal("10.00"), response.getNetChange());
    assertEquals(new BigDecimal("110.00"), response.getBalance());
    assertTrue(response.isWon());
    verify(rpcWalletService).transferFromOperator(wallet, 10.00f);
  }

  @Test
  void spinTreatsZeroAsLossForParityBet() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    stubWalletBalance(wallet, 7, 100.00f);
    when(random.nextInt(37)).thenReturn(0);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "PARITY",
        "EVEN"
    );

    assertEquals(Integer.valueOf(0), response.getWinningNumber());
    assertEquals("GREEN", response.getWinningColor());
    assertEquals(new BigDecimal("0.00"), response.getPayout());
    assertEquals(new BigDecimal("-10.00"), response.getNetChange());
    assertFalse(response.isWon());
    verify(rpcWalletService).transferToOperator(wallet, 10.00f);
  }

  @Test
  void spinReturnsWinWhenRangeBetMatches() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    stubWalletBalance(wallet, 7, 100.00f);
    when(random.nextInt(37)).thenReturn(29);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "RANGE",
        "HIGH"
    );

    assertEquals(Integer.valueOf(29), response.getWinningNumber());
    assertEquals("BLACK", response.getWinningColor());
    assertEquals(new BigDecimal("20.00"), response.getPayout());
    assertEquals(new BigDecimal("10.00"), response.getNetChange());
    assertTrue(response.isWon());
    verify(rpcWalletService).transferFromOperator(wallet, 10.00f);
  }

  @Test
  void spinReturnsWinWhenDozenBetMatches() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    stubWalletBalance(wallet, 7, 100.00f);
    when(random.nextInt(37)).thenReturn(20);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "DOZEN",
        "SECOND12"
    );

    assertEquals(Integer.valueOf(20), response.getWinningNumber());
    assertEquals("BLACK", response.getWinningColor());
    assertEquals(new BigDecimal("30.00"), response.getPayout());
    assertEquals(new BigDecimal("20.00"), response.getNetChange());
    assertEquals(new BigDecimal("120.00"), response.getBalance());
    assertTrue(response.isWon());
    verify(rpcWalletService).transferFromOperator(wallet, 20.00f);
  }

  @Test
  void spinReturnsWinWhenColumnBetMatches() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    stubWalletBalance(wallet, 7, 100.00f);
    when(random.nextInt(37)).thenReturn(36);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("10.00"),
        "COLUMN",
        "THIRD"
    );

    assertEquals(Integer.valueOf(36), response.getWinningNumber());
    assertEquals("RED", response.getWinningColor());
    assertEquals(new BigDecimal("30.00"), response.getPayout());
    assertEquals(new BigDecimal("20.00"), response.getNetChange());
    assertTrue(response.isWon());
    verify(rpcWalletService).transferFromOperator(wallet, 20.00f);
  }

  @Test
  void spinReturnsWinWhenNumberBetMatches() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    stubWalletBalance(wallet, 7, 100.00f);
    when(random.nextInt(37)).thenReturn(17);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("5.00"),
        "NUMBER",
        "17"
    );

    assertEquals(Integer.valueOf(17), response.getWinningNumber());
    assertEquals("BLACK", response.getWinningColor());
    assertEquals("NUMBER", response.getBetType());
    assertEquals("17", response.getBetValue());
    assertEquals(new BigDecimal("180.00"), response.getPayout());
    assertEquals(new BigDecimal("175.00"), response.getNetChange());
    assertEquals(new BigDecimal("275.00"), response.getBalance());
    assertTrue(response.isWon());
    verify(rpcWalletService).transferFromOperator(wallet, 175.00f);
  }

  @Test
  void spinNormalizesNumberBetValue() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    stubWalletBalance(wallet, 7, 100.00f);
    when(random.nextInt(37)).thenReturn(7);

    RouletteSpinResponse response = rouletteService.spin(
        user,
        new BigDecimal("5.00"),
        "NUMBER",
        "07"
    );

    assertEquals("7", response.getBetValue());
    assertTrue(response.isWon());
  }

  @Test
  void spinThrowsWhenBetTypeIsUnsupported() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("10.00"), "SPLIT", "1-2")
    );

    assertEquals("Unsupported bet type", error.getMessage());
    verify(userWalletRepository, never()).findUserWalletByUserId(7);
  }

  @Test
  void spinThrowsWhenColorBetValueIsInvalid() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("10.00"), "COLOR", "GREEN")
    );

    assertEquals("Bet value must be RED or BLACK", error.getMessage());
    verify(userWalletRepository, never()).findUserWalletByUserId(7);
  }

  @Test
  void spinThrowsWhenNumberBetValueIsOutOfRange() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("10.00"), "NUMBER", "40")
    );

    assertEquals("Number bet must be between 0 and 36", error.getMessage());
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

    stubWalletBalance(wallet, 7, 5.00f);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> rouletteService.spin(user, new BigDecimal("10.00"), "COLOR", "RED")
    );

    assertEquals("Insufficient balance", error.getMessage());
    verify(rpcWalletService, never()).transferToOperator(wallet, 10.00f);
    verify(rpcWalletService, never()).transferFromOperator(any(), anyFloat());
  }

  private void stubWalletBalance(UserWallet wallet, Integer userId, float balance) {
    when(userWalletRepository.findUserWalletByUserId(userId)).thenReturn(Optional.of(wallet));
    when(rpcWalletService.getWalletBalance(wallet)).thenReturn(balance);
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
