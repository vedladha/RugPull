package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Represents a user entity within the system.
 *
 * <p>This class maps to the "users" table in the database and manages core user authentication
 * details, audit timestamps, and the relationship to the user's detailed profile.
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Integer userId;

  @Column(nullable = false, unique = true)
  private String email;

  @JsonIgnore
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = false)
  private Boolean deleted = false;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true,
      fetch = FetchType.LAZY)
  private UserProfile userProfile;

  /**
   * Retrieves the unique identifier for this user.
   *
   * @return the user ID
   */
  public Integer getUserId() {
    return userId;
  }

  /**
   * Sets the unique identifier for this user.
   *
   * @param userId the ID to set
   */
  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  /**
   * Retrieves the email address associated with this user.
   *
   * @return the user's email address
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email address for this user.
   *
   * @param email the email address to set
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Retrieves the hashed password of this user.
   *
   * <p>This field is ignored during JSON serialization for security purposes.
   *
   * @return the hashed password
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Sets the hashed password for this user.
   *
   * @param passwordHash the hashed password to set
   */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * Retrieves the timestamp of when this user record was created.
   *
   * <p>This value is managed by the database and cannot be updated manually.
   *
   * @return the creation timestamp
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Retrieves the timestamp of when this user record was last updated.
   *
   * <p>This value is managed by the database and cannot be updated manually.
   *
   * @return the last update timestamp
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Retrieves the soft deletion status of this user.
   *
   * @return true if the user is marked as deleted, false otherwise
   */
  public Boolean getDeleted() {
    return deleted;
  }

  /**
   * Sets the soft deletion status for this user.
   *
   * @param deleted the deletion status to set
   */
  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  /**
   * Retrieves the profile associated with this user.
   *
   * @return the associated {@link UserProfile}
   */
  public UserProfile getUserProfile() {
    return userProfile;
  }

  /**
   * Sets the profile associated with this user.
   *
   * @param userProfile the {@link UserProfile} to associate with this user
   */
  public void setUserProfile(UserProfile userProfile) {
    this.userProfile = userProfile;
  }
}
