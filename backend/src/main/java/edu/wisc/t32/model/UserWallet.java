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
 * Represents a user's digital wallet within the system.
 *
 * <p>This entity maps to the "user_wallets" table and shares its primary key with the
 * associated {@link User} entity through a one-to-one relationship.
 */
@Entity
@Table(name = "user_wallets")
public class UserWallet {

  @Id
  @Column(name = "user_id")
  private Integer userId;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "wallet_address", nullable = false, unique = true)
  private String walletAddress;

  @JsonIgnore
  @Column(name = "wallet_private_key", nullable = false)
  private String walletPrivateKey;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  /**
   * Retrieves the unique identifier for this wallet.
   *
   * <p>This ID corresponds directly to the associated {@link User} ID.
   *
   * @return the wallet ID
   */
  public Integer getUserId() {
    return userId;
  }

  /**
   * Sets the unique identifier for this wallet.
   *
   * @param userId the ID to set
   */
  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  /**
   * Retrieves the user associated with this wallet.
   *
   * <p>This relationship is ignored during JSON serialization to prevent infinite recursion.
   *
   * @return the associated {@link User}
   */
  public User getUser() {
    return user;
  }

  /**
   * Sets the user associated with this wallet.
   *
   * @param user the {@link User} to associate
   */
  public void setUser(User user) {
    this.user = user;
  }

  /**
   * Retrieves the public address of this wallet.
   *
   * @return the wallet address
   */
  public String getWalletAddress() {
    return walletAddress;
  }

  /**
   * Sets the public address for this wallet.
   *
   * @param walletAddress the wallet address to set
   */
  public void setWalletAddress(String walletAddress) {
    this.walletAddress = walletAddress;
  }

  /**
   * Retrieves the private key associated with this wallet.
   *
   * <p>This field is ignored during JSON serialization for security purposes.
   *
   * @return the wallet's private key
   */
  public String getWalletPrivateKey() {
    return walletPrivateKey;
  }

  /**
   * Sets the private key for this wallet.
   *
   * @param walletPrivateKey the private key to set
   */
  public void setWalletPrivateKey(String walletPrivateKey) {
    this.walletPrivateKey = walletPrivateKey;
  }

  /**
   * Retrieves the timestamp of when this wallet record was created.
   *
   * <p>This value is managed by the database and cannot be manually updated.
   *
   * @return the creation timestamp
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Retrieves the timestamp of when this wallet record was last updated.
   *
   * <p>This value is managed by the database and cannot be manually updated.
   *
   * @return the last update timestamp
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
