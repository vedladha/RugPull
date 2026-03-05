package com.example.demo.services;

import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;


@Service
public class AuthService {

    UserRepository userRepo;

    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /****
     * Register method - creates a new user account with the provided display name, email, and password.
     * @param displayName - the display name for the user's profile
     * @param email - the email address for the user's account, which must be unique
     * @param password - the password for the user's account.
     * @return - returns the created User object with the associated UserProfile.
     */
    public User register(String displayName, String email, String password) {
        // TODO: Implement password hashing and salting in a future implementation. For now, we're storing passwords in plain text for initial testing.
        /*SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password + salt);
        System.out.println("Generated salt: " + salt);
        System.out.println("Hashed password: " + hashedPassword);*/

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setDeleted(false);
        
        UserProfile profile = new UserProfile();
        profile.setDisplayName(displayName);
        profile.setUser(user);

        user.setUserProfile(profile);

        return userRepo.save(user);
    }

    /****
     * Login method - authenticates the user by checking if the provided email exists and if the password matches the stored password hash.
     * Currently, since we're storing passwords in plain text for initial testing, it simply compares the provided password with the stored password hash. 
     * In a future implementation, this will be updated to hash the provided password with the stored salt and compare it to the stored password hash.
     * @param email - the email address provided by the user for authentication
     * @param password - the password provided by the user for authentication
     * @return - returns the authenticated User object if the email and password are valid, or throws an exception if the authentication fails
     */
    public User login(String email, String password) {
        return userRepo.findByEmail(email)
                .filter(user -> {
                    //BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                    //return encoder.matches(password + salt, user.getPasswordHash());
                    return user.getPasswordHash().equals(password);
                })
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
    }
}
