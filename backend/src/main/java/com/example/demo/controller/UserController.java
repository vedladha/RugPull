package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserController(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    
    @PostMapping("/users")
    public ResponseEntity<?> addNewUser(@RequestParam String email,
                                        @RequestParam String passwordHash,
                                        @RequestParam String passwordSalt) {

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordHash);
        u.setPasswordSalt(passwordSalt);
        u.setDeleted(false);

        return ResponseEntity.ok(userRepository.save(u));
    }

    
    @PutMapping("/users/{userId}/profile")
    public ResponseEntity<?> upsertProfile(@PathVariable Integer userId,
                                           @RequestParam(required = false) String displayName,
                                           @RequestParam(required = false) String bio) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        UserProfile profile = userProfileRepository.findById(userId).orElse(new UserProfile());
        profile.setUser(user);
        profile.setDisplayName(displayName);
        profile.setBio(bio);

        return ResponseEntity.ok(userProfileRepository.save(profile));
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<?> getProfile(@PathVariable Integer userId) {
        return userProfileRepository.findById(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
