import org.springframework.stereotype.Indexed;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

@Entity
@Table(name = "UserProfiles")
public class UserProfile {
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(nullable = false, name = "display_name")
    private String displayName;

    @Column
    private String bio;

    @CreationTimestamp
    @Column(updatable = false, name="created_at")
    private Date createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private Date updatedAt;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Date getCreatedAt() { return createdAt; }

    public Date getUpdatedAt() { return updatedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
