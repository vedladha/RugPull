package edu.wisc.t32.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.wisc.t32.dto.SlotSpinResponse;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.UserWalletRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotMachineServiceTest {
  @Mock
  private UserWalletRepository userWalletRepository;

  @Mock
  private RpcWalletService rpcWalletService;

  @Mock
  private Random random;

  @InjectMocks
  private SlotMachineService slotMachineService;

  @Test
  void spinReturnsLossWhenReelsDoNotMatch() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(wallet));
    when(rpcWalletService.getWalletBalance(wallet)).thenReturn(100.00f);
    when(random.nextInt(5)).thenReturn(0, 1, 2);

    SlotSpinResponse response = slotMachineService.spin(user, new BigDecimal("10.00"));

    assertEquals(List.of("CHERRY", "LEMON", "BAR"), response.getReels());
    assertEquals(new BigDecimal("10.00"), response.getWager());
    assertEquals(new BigDecimal("0.00"), response.getPayout());
    assertEquals(new BigDecimal("-10.00"), response.getNetChange());
    assertEquals(new BigDecimal("90.00"), response.getBalance());
    assertEquals(false, response.isWon());
    verify(rpcWalletService).transferToOperator(wallet, 10.00f);
    verify(rpcWalletService, never()).transferFromOperator(wallet, 0.00f);
  }

  @Test
  void spinReturnsWinWhenReelsMatch() {
    User user = buildUser(7);
    UserWallet wallet = buildWallet(7);

    when(userWalletRepository.findUserWalletByUserId(7)).thenReturn(Optional.of(wallet));
    when(rpcWalletService.getWalletBalance(wallet)).thenReturn(100.00f);
    when(random.nextInt(5)).thenReturn(4, 4, 4);

    SlotSpinResponse response = slotMachineService.spin(user, new BigDecimal("10.00"));

    assertEquals(List.of("SEVEN", "SEVEN", "SEVEN"), response.getReels());
    assertEquals(new BigDecimal("100.00"), response.getPayout());
    assertEquals(new BigDecimal("90.00"), response.getNetChange());
    assertEquals(new BigDecimal("190.00"), response.getBalance());
    assertEquals(true, response.isWon());
    verify(rpcWalletService).transferFromOperator(wallet, 90.00f);
    verify(rpcWalletService, never()).transferToOperator(wallet, 10.00f);
  }

  @Test
  void spinThrowsWhenWagerIsInvalid() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> slotMachineService.spin(user, new BigDecimal("0.00"))
    );

    assertEquals("Wager must be greater than 0", error.getMessage());
    verify(userWalletRepository, never()).findUserWalletByUserId(7);
  }

  @Test
  void spinThrowsWhenWagerHasTooManyDecimals() {
    User user = buildUser(7);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> slotMachineService.spin(user, new BigDecimal("1.001"))
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
        () -> slotMachineService.spin(user, new BigDecimal("10.00"))
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
