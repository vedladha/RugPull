package edu.wisc.t32.services;

import edu.wisc.t32.dto.SlotSpinResponse;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.UserWalletRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for spinning the earn-page slot machine and settling the user's RPC balance.
 */
@Service
public class SlotMachineService {
  private static final List<String> SYMBOLS = List.of("CHERRY", "LEMON", "BAR", "STAR", "SEVEN");
  private static final int SCALE = 2;

  private final UserWalletRepository userWalletRepository;
  private final RpcWalletService rpcWalletService;
  private final Random random;

  /**
   * Creates the slot machine service with production randomness.
   *
   * @param userWalletRepository repository used to load the authenticated user's wallet
   * @param rpcWalletService service used to query and transfer RPC balances
   */
  @Autowired
  public SlotMachineService(
      UserWalletRepository userWalletRepository,
      RpcWalletService rpcWalletService) {
    this(userWalletRepository, rpcWalletService, new SecureRandom());
  }

  SlotMachineService(
      UserWalletRepository userWalletRepository,
      RpcWalletService rpcWalletService,
      Random random) {
    this.userWalletRepository = userWalletRepository;
    this.rpcWalletService = rpcWalletService;
    this.random = random;
  }

  /**
   * Spins the slot machine for the given user and settles the resulting RPC balance change.
   *
   * @param user the authenticated user placing the wager
   * @param wager the wager amount for the spin
   * @return the spin result, including reels, payout, and updated balance
   */
  public SlotSpinResponse spin(User user, BigDecimal wager) {
    BigDecimal normalizedWager = normalizeWager(wager);
    UserWallet userWallet = loadUserWallet(user);
    BigDecimal startingBalance = toCurrency(rpcWalletService.getWalletBalance(userWallet));
    if (startingBalance.compareTo(normalizedWager) < 0) {
      throw new IllegalArgumentException("Insufficient balance");
    }

    List<String> reels = List.of(nextSymbol(), nextSymbol(), nextSymbol());
    BigDecimal payout = calculatePayout(normalizedWager, reels);
    BigDecimal netChange = payout.subtract(normalizedWager).setScale(SCALE, RoundingMode.HALF_UP);
    settleBalance(userWallet, netChange);

    SlotSpinResponse response = new SlotSpinResponse();
    response.setReels(reels);
    response.setWager(normalizedWager);
    response.setPayout(payout);
    response.setNetChange(netChange);
    response.setBalance(startingBalance.add(netChange).setScale(SCALE, RoundingMode.HALF_UP));
    response.setWon(netChange.compareTo(BigDecimal.ZERO) > 0);
    response.setMessage(response.isWon() ? "You won!" : "No win this spin.");
    return response;
  }

  private BigDecimal normalizeWager(BigDecimal wager) {
    if (wager == null) {
      throw new IllegalArgumentException("Wager is required");
    }
    if (wager.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Wager must be greater than 0");
    }
    if (wager.scale() > SCALE) {
      throw new IllegalArgumentException("Wager cannot have more than 2 decimal places");
    }
    return wager.setScale(SCALE, RoundingMode.HALF_UP);
  }

  private UserWallet loadUserWallet(User user) {
    Optional<UserWallet> userWallet = userWalletRepository.findUserWalletByUserId(user.getUserId());
    return userWallet.orElseThrow(
        () -> new IllegalStateException("Internal service error fetching wallet")
    );
  }

  private String nextSymbol() {
    return SYMBOLS.get(random.nextInt(SYMBOLS.size()));
  }

  private BigDecimal calculatePayout(BigDecimal wager, List<String> reels) {
    if (!allReelsMatch(reels)) {
      return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    BigDecimal multiplier = switch (reels.getFirst()) {
      case "SEVEN" -> BigDecimal.valueOf(10);
      case "STAR" -> BigDecimal.valueOf(6);
      case "BAR" -> BigDecimal.valueOf(4);
      case "CHERRY" -> BigDecimal.valueOf(3);
      default -> BigDecimal.valueOf(2);
    };

    return wager.multiply(multiplier).setScale(SCALE, RoundingMode.HALF_UP);
  }

  private boolean allReelsMatch(List<String> reels) {
    return reels.getFirst().equals(reels.get(1)) && reels.getFirst().equals(reels.get(2));
  }

  private void settleBalance(UserWallet userWallet, BigDecimal netChange) {
    if (netChange.compareTo(BigDecimal.ZERO) < 0) {
      rpcWalletService.transferToOperator(userWallet, netChange.abs().floatValue());
    } else if (netChange.compareTo(BigDecimal.ZERO) > 0) {
      rpcWalletService.transferFromOperator(userWallet, netChange.floatValue());
    }
  }

  private BigDecimal toCurrency(float amount) {
    return new BigDecimal(Float.toString(amount)).setScale(SCALE, RoundingMode.HALF_UP);
  }
}
