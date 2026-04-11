package edu.wisc.t32.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entity representing a daily_reward entry in the database.
 * Mapped to the "daily_reward" table with a unique constraint on user_id.
 */
@Entity
@Table(name = "daily_rewards")
public class DailyReward {

  @Id
  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "claimed_last", nullable = false)
  private LocalDateTime claimedLast;

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public LocalDateTime getClaimedLast() {
    return claimedLast;
  }

  public void setClaimedLast(LocalDateTime claimedLast) {
    this.claimedLast = claimedLast;
  }
}
