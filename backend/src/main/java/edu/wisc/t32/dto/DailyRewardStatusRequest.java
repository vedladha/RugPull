package edu.wisc.t32.dto;

/**
 * Represents a daily reward status request.
 */
public class DailyRewardStatusRequest {
  private boolean claimed = false;

  public void setClaimed(boolean claimed) {
    this.claimed = claimed;
  }

  public boolean isClaimed() {
    return claimed;
  }

  /**
   * Creates a new daily reward status request.
   *
   * @return the newly created status request
   */
  public static DailyRewardStatusRequest next() {
    return new DailyRewardStatusRequest();
  }
}
