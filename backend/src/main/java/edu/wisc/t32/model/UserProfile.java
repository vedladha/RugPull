package edu.wisc.t32.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Represents a user's detailed profile within the system.
 *
 * <p>This entity maps to the "UserProfiles" table and shares its primary key with the
 * associated {@link User} entity through a one-to-one relationship.
 */
@Entity
@Table(name = "UserProfiles")
public class UserProfile {

  @Id
  @Column(name = "user_id")
  private Integer userId;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "display_name", unique = true)
  private String displayName;

  @Column(columnDefinition = "TEXT")
  private String bio;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  /**
   * Retrieves the unique identifier for this profile.
   *
   * <p>This ID corresponds directly to the associated {@link User} ID.
   *
   * @return the profile ID
   */
  public Integer getUserId() {
    return userId;
  }

  /**
   * Sets the unique identifier for this profile.
   *
   * @param userId the ID to set
   */
  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  /**
   * Retrieves the user associated with this profile.
   *
   * <p>This relationship is ignored during JSON serialization to prevent infinite recursion.
   *
   * @return the associated {@link User}
   */
  public User getUser() {
    return user;
  }

  /**
   * Sets the user associated with this profile.
   *
   * @param user the {@link User} to associate
   */
  public void setUser(User user) {
    this.user = user;
  }

  /**
   * Retrieves the unique display name for this user profile.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the display name for this user profile.
   *
   * @param displayName the display name to set
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Retrieves the biographical text for this user profile.
   *
   * @return the user's bio
   */
  public String getBio() {
    return bio;
  }

  /**
   * Sets the biographical text for this user profile.
   *
   * @param bio the bio to set
   */
  public void setBio(String bio) {
    this.bio = bio;
  }

  /**
   * Retrieves the timestamp of when this profile was created.
   *
   * <p>This value is managed by the database and cannot be manually updated.
   *
   * @return the creation timestamp
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Retrieves the timestamp of when this profile was last updated.
   *
   * <p>This value is managed by the database and cannot be manually updated.
   *
   * @return the last update timestamp
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
