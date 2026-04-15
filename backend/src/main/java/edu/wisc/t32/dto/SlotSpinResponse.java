package edu.wisc.t32.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response payload describing the outcome of a slot machine spin.
 */
public class SlotSpinResponse {
  private List<String> reels;
  private BigDecimal wager;
  private BigDecimal payout;
  private BigDecimal netChange;
  private BigDecimal balance;
  private boolean won;
  private String message;

  public List<String> getReels() {
    return reels;
  }

  public void setReels(List<String> reels) {
    this.reels = reels;
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
