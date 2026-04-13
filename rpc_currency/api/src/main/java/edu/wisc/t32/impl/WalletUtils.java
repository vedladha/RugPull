package edu.wisc.t32.impl;

import java.math.BigDecimal;

/**
 * Various utilities for wallet API classes.
 */
class WalletUtils {

  /**
   * Converts a raw integer/long balance into a human-readable floating-point representation.
   *
   * <p>This is typically used to convert a token's smallest atomic unit (e.g., Tinybars)
   * into its standard display format by shifting the decimal point to the left.
   *
   * @param balance  The raw token balance in its smallest long unit.
   * @param decimals The number of decimal places configured for this specific token.
   * @return The formatted floating-point representation of the balance.
   */
  public static float longToFloat(long balance, int decimals) {
    BigDecimal decimalized = new BigDecimal(balance).movePointLeft(decimals);
    return decimalized.floatValue();
  }

  /**
   * Converts a human-readable floating-point amount into its raw integer/long representation,
   * while enforcing a strict limit on the number of allowable decimal places.
   *
   * <p>This method prevents silent precision loss by throwing an exception if the input
   * amount contains more decimal places than the network or token allows.
   *
   * @param amount   The floating-point amount to convert.
   * @param decimals The number of decimal places configured for the token.
   * @return The raw long representation of the amount, scaled by {@code maxDecimals}.
   * @throws IllegalArgumentException if the {@code amount} contains more fractional
   *                                  digits than {@code maxDecimals}.
   */
  public static long floatToLong(float amount, int decimals) {
    BigDecimal decimal = new BigDecimal(Float.toString(amount));
    long flotaAmount;
    // we do this to get rid of all decimals. We can have at most maxDecimals so throw if invalid
    // input
    try {
      flotaAmount = decimal.movePointRight(decimals).longValueExact();
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          "input amount is larger than %d decimal places".formatted(decimals));
    }

    return flotaAmount;
  }

  /**
   * Asserts that a given passed object is not null.
   *
   * @param check         checks if the value is null
   * @param className     the class name of the call
   * @param parameterName the parameterName of the call
   * @param methodName    the methodName of the call
   */
  public static void assertNotNull(Object check, String className, String parameterName,
                                   String methodName) {
    if (check == null) {
      throw new IllegalArgumentException(
          parameterName + " can not be null for " + className + "#" + methodName);
    }
  }

}
