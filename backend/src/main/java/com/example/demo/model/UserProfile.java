
package com.example.demo.model;
import com.fasterxml.jackson.annotation.JsonIgnore;


import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
