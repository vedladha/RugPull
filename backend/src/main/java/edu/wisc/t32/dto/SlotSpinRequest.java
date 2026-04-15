package edu.wisc.t32.dto;

import java.math.BigDecimal;

/**
 * Request payload for spinning the slot machine.
 */
public class SlotSpinRequest {
  private BigDecimal wager;

  public BigDecimal getWager() {
    return wager;
  }

  public void setWager(BigDecimal wager) {
    this.wager = wager;
  }
}
