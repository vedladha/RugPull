package edu.wisc.t32.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a secure, server-side tracking session for an in-progress advertisement.
 * This prevents clients from spoofing ad completions by enforcing strict server-side
 * duration checks.
 */
@Entity
@Table(name = "ad_sessions")
public class AdSession {

  // Generate a hard-to-guess UUID ticket the moment the session starts
  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private String id = UUID.randomUUID().toString();

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "ad_title", nullable = false)
  private String adTitle;

  @Column(name = "required_duration_seconds", nullable = false)
  private int requiredDurationSeconds;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "is_claimed", nullable = false)
  private boolean isClaimed = false;

  // --- Getters and Setters ---

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getAdTitle() {
    return adTitle;
  }

  public void setAdTitle(String adTitle) {
    this.adTitle = adTitle;
  }

  public int getRequiredDurationSeconds() {
    return requiredDurationSeconds;
  }

  public void setRequiredDurationSeconds(int requiredDurationSeconds) {
    this.requiredDurationSeconds = requiredDurationSeconds;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public boolean isClaimed() {
    return isClaimed;
  }

  public void setClaimed(boolean claimed) {
    this.isClaimed = claimed;
  }
}
