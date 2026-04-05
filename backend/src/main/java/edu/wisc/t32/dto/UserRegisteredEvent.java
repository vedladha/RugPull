package edu.wisc.t32.dto;

import edu.wisc.t32.model.UserProfile;
import edu.wisc.t32.enums.UserStatus;
import java.time.LocalDateTime;

/**
 * Event DTO triggered when a new user successfully registers in the system.
 */
public record UserRegisteredEvent(
    Integer userId,
    String email,
    UserStatus status,
    UserProfile userProfile,
    LocalDateTime registeredAt
) {}
