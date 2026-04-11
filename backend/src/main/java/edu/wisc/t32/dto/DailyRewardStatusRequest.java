package edu.wisc.t32.dto;

/**
 * Represents a daily reward status request.
 */
public class DailyRewardStatusRequest {
  private boolean claimed = false;
  private int streak = 0;
  private float nextReward = 0;
  private long length = 0;

  public void setLength(long length) {
    this.length = length;
  }

  public long getLength() {
    return length;
  }

  public void setStreak(int streak) {
    this.streak = streak;
  }

  public int getStreak() {
    return streak;
  }

  public void setNextReward(float nextReward) {
    this.nextReward = nextReward;
  }

  public float getNextReward() {
    return nextReward;
  }

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
