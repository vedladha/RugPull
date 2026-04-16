package edu.wisc.t32.dto;

import java.math.BigDecimal;

/**
 * Response payload describing the outcome of a roulette spin.
 */
public class RouletteSpinResponse {
  private Integer winningNumber;
  private String winningColor;
  private String betType;
  private String betValue;
  private BigDecimal wager;
  private BigDecimal payout;
  private BigDecimal netChange;
  private BigDecimal balance;
  private boolean won;
  private String message;

  public Integer getWinningNumber() {
    return winningNumber;
  }

  public void setWinningNumber(Integer winningNumber) {
    this.winningNumber = winningNumber;
  }

  public String getWinningColor() {
    return winningColor;
  }

  public void setWinningColor(String winningColor) {
    this.winningColor = winningColor;
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

  public BigDecimal getWager() {
    return wager;
  }

  public void setWager(BigDecimal wager) {
    this.wager = wager;
  }

  public BigDecimal getPayout() {
    return payout;
  }

  public void setPayout(BigDecimal payout) {
    this.payout = payout;
  }

  public BigDecimal getNetChange() {
    return netChange;
  }

  public void setNetChange(BigDecimal netChange) {
    this.netChange = netChange;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public boolean isWon() {
    return won;
  }

  public void setWon(boolean won) {
    this.won = won;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
