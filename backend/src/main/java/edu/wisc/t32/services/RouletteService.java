package edu.wisc.t32.services;

import edu.wisc.t32.dto.RouletteSpinResponse;
import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import edu.wisc.t32.repository.UserWalletRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for spinning the earn-page roulette wheel and settling the user's RPC balance.
 */
@Service
public class RouletteService {
  private static final Set<Integer> RED_NUMBERS = Set.of(
      1, 3, 5, 7, 9, 12, 14, 16, 18,
      19, 21, 23, 25, 27, 30, 32, 34, 36
  );
  private static final String COLOR_BET_TYPE = "COLOR";
  private static final Set<String> COLOR_BET_VALUES = Set.of("RED", "BLACK");
  private static final int SCALE = 2;

  private final UserWalletRepository userWalletRepository;
  private final RpcWalletService rpcWalletService;
  private final Random random;

  /**
   * Creates the roulette service with production randomness.
   *
   * @param userWalletRepository repository used to load the authenticated user's wallet
   * @param rpcWalletService service used to query and transfer RPC balances
   */
  @Autowired
  public RouletteService(
      UserWalletRepository userWalletRepository,
      RpcWalletService rpcWalletService) {
    this(userWalletRepository, rpcWalletService, new SecureRandom());
  }

  RouletteService(
      UserWalletRepository userWalletRepository,
      RpcWalletService rpcWalletService,
      Random random) {
    this.userWalletRepository = userWalletRepository;
    this.rpcWalletService = rpcWalletService;
    this.random = random;
  }

  /**
   * Spins the roulette wheel for the given user and settles the resulting RPC balance change.
   *
   * @param user the authenticated user placing the wager
   * @param wager the wager amount for the spin
   * @param betType the type of wager being placed
   * @param betValue the selected wager value
   * @return the spin result, including winning number, color, payout, and updated balance
   */
  public RouletteSpinResponse spin(User user, BigDecimal wager, String betType, String betValue) {
    BigDecimal normalizedWager = normalizeWager(wager);
    String normalizedBetType = normalizeBetType(betType);
    String normalizedBetValue = normalizeBetValue(normalizedBetType, betValue);

    UserWallet userWallet = loadUserWallet(user);
    BigDecimal startingBalance = toCurrency(rpcWalletService.getWalletBalance(userWallet));
    if (startingBalance.compareTo(normalizedWager) < 0) {
      throw new IllegalArgumentException("Insufficient balance");
    }

    int winningNumber = random.nextInt(37);
    String winningColor = determineColor(winningNumber);
    boolean won = didWin(normalizedBetType, normalizedBetValue, winningColor);
    BigDecimal payout = calculatePayout(normalizedWager, won);
    BigDecimal netChange = payout.subtract(normalizedWager).setScale(SCALE, RoundingMode.HALF_UP);
    settleBalance(userWallet, netChange);

    RouletteSpinResponse response = new RouletteSpinResponse();
    response.setWinningNumber(winningNumber);
    response.setWinningColor(winningColor);
    response.setBetType(normalizedBetType);
    response.setBetValue(normalizedBetValue);
    response.setWager(normalizedWager);
    response.setPayout(payout);
    response.setNetChange(netChange);
    response.setBalance(startingBalance.add(netChange).setScale(SCALE, RoundingMode.HALF_UP));
    response.setWon(won);
    response.setMessage(buildMessage(won, winningNumber, winningColor));
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

  private String normalizeBetType(String betType) {
    if (betType == null || betType.isBlank()) {
      throw new IllegalArgumentException("Bet type is required");
    }

    String normalizedBetType = betType.trim().toUpperCase(Locale.ROOT);
    if (!COLOR_BET_TYPE.equals(normalizedBetType)) {
      throw new IllegalArgumentException("Unsupported bet type");
    }
    return normalizedBetType;
  }

  private String normalizeBetValue(String betType, String betValue) {
    if (betValue == null || betValue.isBlank()) {
      throw new IllegalArgumentException("Bet value is required");
    }

    String normalizedBetValue = betValue.trim().toUpperCase(Locale.ROOT);
    if (COLOR_BET_TYPE.equals(betType) && !COLOR_BET_VALUES.contains(normalizedBetValue)) {
      throw new IllegalArgumentException("Bet value must be RED or BLACK");
    }
    return normalizedBetValue;
  }

  private UserWallet loadUserWallet(User user) {
    Optional<UserWallet> userWallet = userWalletRepository.findUserWalletByUserId(user.getUserId());
    return userWallet.orElseThrow(
        () -> new IllegalStateException("Internal service error fetching wallet")
    );
  }

  private String determineColor(int winningNumber) {
    if (winningNumber == 0) {
      return "GREEN";
    }
    return RED_NUMBERS.contains(winningNumber) ? "RED" : "BLACK";
  }

  private boolean didWin(String betType, String betValue, String winningColor) {
    return COLOR_BET_TYPE.equals(betType) && betValue.equals(winningColor);
  }

  private BigDecimal calculatePayout(BigDecimal wager, boolean won) {
    if (!won) {
      return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }
    return wager.multiply(BigDecimal.valueOf(2)).setScale(SCALE, RoundingMode.HALF_UP);
  }

  private void settleBalance(UserWallet userWallet, BigDecimal netChange) {
    if (netChange.compareTo(BigDecimal.ZERO) < 0) {
      rpcWalletService.transferToOperator(userWallet, netChange.abs().floatValue());
    } else if (netChange.compareTo(BigDecimal.ZERO) > 0) {
      rpcWalletService.transferFromOperator(userWallet, netChange.floatValue());
    }
  }

  private String buildMessage(boolean won, int winningNumber, String winningColor) {
    String outcome = won ? "You won. " : "No win. ";
    return outcome + "The wheel landed on " + winningNumber + " " + winningColor + ".";
  }

  private BigDecimal toCurrency(float amount) {
    return new BigDecimal(Float.toString(amount)).setScale(SCALE, RoundingMode.HALF_UP);
  }
}
