package edu.wisc.t32.impl;

/**
 * Various utilities for wallet API classes.
 */
class WalletUtils {

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
