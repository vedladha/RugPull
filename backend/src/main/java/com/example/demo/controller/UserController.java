package com.example.demo.controller;

import com.example.demo.dto.AuthLoginRequest;
import com.example.demo.dto.AuthSignupRequest;
import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.model.UserWallet;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserWalletRepository;
import com.example.demo.service.PasswordService;
import com.example.demo.service.RpcWalletService;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserWalletRepository userWalletRepository;
    private final PasswordService passwordService;
    private final RpcWalletService rpcWalletService;

    public UserController(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserWalletRepository userWalletRepository,
            PasswordService passwordService,
            RpcWalletService rpcWalletService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userWalletRepository = userWalletRepository;
        this.passwordService = passwordService;
        this.rpcWalletService = rpcWalletService;
    }

    @PostMapping("/auth/signup")
    @Transactional
    public ResponseEntity<?> signUp(@RequestBody AuthSignupRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();
        String displayName =
                request.getDisplayName() == null ? null : request.getDisplayName().trim();

        if (email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body("email and password are required");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        String salt = passwordService.generateSalt();
        String hash = passwordService.hashPassword(password, salt);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hash);
        user.setPasswordSalt(salt);
        user.setDeleted(false);
        user = userRepository.save(user);

        if (displayName != null && !displayName.isBlank()) {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setDisplayName(displayName);
            profile.setBio(null);
            userProfileRepository.save(profile);
        }

        final RpcWalletService.WalletCredentials walletCredentials;
        try {
            walletCredentials = rpcWalletService.createWallet();
        } catch (IllegalStateException e) {
            LOGGER.error("Signup failed while creating wallet for {}", email, e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(
                            Map.of(
                                    "error", "Could not create wallet for new user",
                                    "details", e.getMessage()));
        }

        UserWallet wallet = new UserWallet();
        wallet.setUser(user);
        wallet.setWalletAddress(walletCredentials.walletId());
        userWalletRepository.save(wallet);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("user", buildUserPayload(user)));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthLoginRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        User user = maybeUser.get();
        boolean valid =
                passwordService.verifyPassword(
                        password, user.getPasswordSalt(), user.getPasswordHash());
        if (!valid || Boolean.TRUE.equals(user.getDeleted())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        return ResponseEntity.ok(Map.of("user", buildUserPayload(user)));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out"));
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

    private Map<String, Object> buildUserPayload(User user) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.getUserId());
        payload.put("email", user.getEmail());

        Optional<UserProfile> profile = userProfileRepository.findById(user.getUserId());
        payload.put("displayName", profile.map(UserProfile::getDisplayName).orElse(null));

        Optional<UserWallet> wallet = userWalletRepository.findById(user.getUserId());
        payload.put("walletAddress", wallet.map(UserWallet::getWalletAddress).orElse(null));
        return payload;
    }
}
