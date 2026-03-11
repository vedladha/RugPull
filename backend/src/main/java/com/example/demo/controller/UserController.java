package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.model.UserWallet;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserWalletRepository;
import com.example.demo.services.RpcWalletService;
import com.example.demo.services.AuthService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final UserWalletRepository userWalletRepository;
  private final AuthService authService;
  private final RpcWalletService rpcWalletService;

  public UserController(UserRepository userRepository, UserProfileRepository userProfileRepository,
                        UserWalletRepository userWalletRepository,
                        AuthService authService, RpcWalletService rpcWalletService) {
    this.userRepository = userRepository;
    this.userWalletRepository = userWalletRepository;
    this.userProfileRepository = userProfileRepository;
    this.authService = authService;
    this.rpcWalletService = rpcWalletService;
  }

  @GetMapping("/users")
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  @PostMapping("/users/addUser")
  public ResponseEntity<?> addNewUser(@RequestParam String email,
                                      @RequestParam String passwordHash
  ) {
    if (userRepository.findByEmail(email).isPresent()) {
      return ResponseEntity.badRequest().body("Email already exists");
    }

    User u = new User();
    u.setEmail(email);
    u.setPasswordHash(passwordHash);
    u.setDeleted(false);
    return ResponseEntity.ok(userRepository.save(u));
  }

  @PutMapping("/users/{userId}/profile")
  public ResponseEntity<?> upsertProfile(@PathVariable Integer userId,
                                         @RequestParam(required = false) String displayName,
                                         @RequestParam(required = false) String bio) {

    User user = userRepository.findById(userId).orElse(null);
    if (user == null)
      return ResponseEntity.notFound().build();

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
