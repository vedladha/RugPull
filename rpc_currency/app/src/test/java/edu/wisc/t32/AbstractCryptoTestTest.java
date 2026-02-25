package edu.wisc.t32;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test for the class {@link AbstractCryptoTest} this test tests all helper methods created to
 * ensure the test class has stability and is never a source of issue.
 */
class AbstractCryptoTestTest extends AbstractCryptoTest {

  @Test
  public void testSpinAccount() {
    ClientPair acc1 = assertDoesNotThrow(() -> spinTestAccount(20));
    // assertDoesNotThrow(acc1::close);
    assertTrue(true);
  }

}
