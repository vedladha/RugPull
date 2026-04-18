package edu.wisc.t32.dto;

import java.math.BigDecimal;

/**
 * Request payload for placing a roulette wager.
 */
public class RouletteSpinRequest {
  private BigDecimal wager;
  private String betType;
  private String betValue;

  public BigDecimal getWager() {
    return wager;
  }

  public void setWager(BigDecimal wager) {
    this.wager = wager;
  }

  public String getBetType() {
    return betType;
  }

  public void setBetType(String betType) {
    this.betType = betType;
  }

  public String getBetValue() {
    return betValue;
  }

  public void setBetValue(String betValue) {
    this.betValue = betValue;
  }
}
