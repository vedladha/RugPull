package com.example.demo.controller;

import com.example.demo.model.UserWallet;
import com.example.demo.repository.UserWalletRepository;
import com.example.demo.services.AuthService;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.model.User;
//import com.example.demo.util.JwtUtil;
import com.example.demo.services.RpcWalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

//TODO: Implement JWT token generation and validation in the AuthService and JwtUtil classes, and update the login and profile endpoints accordingly to use JWT for authentication and authorization.
@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    AuthService authService;
    RpcWalletService walletService;
    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    UserWalletRepository userWalletRepository;

    /*****
     * Constructor for AuthController - initializes the AuthService and UserRepository dependencies.
     * @param authService - the service responsible for handling authentication logic such as registration and login
     * @param userRepository - the repository used to interact with the User data in the database, such as checking for existing emails during registration and retrieving user information during login
     * @param userProfileRepository - the repository used to interact with the UserProfile data in the database, such as retrieving user profile information during login
     */
    public AuthController(AuthService authService, RpcWalletService walletService, UserRepository userRepository, UserProfileRepository userProfileRepository, UserWalletRepository userWalletRepository) {
        this.authService = authService;
        this.walletService = walletService;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userWalletRepository = userWalletRepository;
    }

    /*****
     * Register endpoint - creates a new user account with the provided display name, email, and password. 
     * The password is currently stored in plain text for initial setup and testing, but will be hashed and salted in a future implementation.
     * @param body - contains the username, email, and password for registration
     * @return - returns the user's email and display name on successful registration, or an error
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String displayName = body.get("displayName");
        String email = body.get("email");
        String password = body.get("password");

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
        }

        if (userProfileRepository.findByDisplayName(displayName).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Display name already in use"));
        }

        User user = authService.register(displayName, email, password);
        final RpcWalletService.WalletCredentials walletCredentials;
        try {
            walletCredentials = walletService.createWallet();
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
        wallet.setWalletPrivateKey(walletCredentials.walletPrivateKey());
        userWalletRepository.save(wallet);
        return ResponseEntity.ok(Map.of("email", user.getEmail(), "displayName", user.getUserProfile().getDisplayName()));
    }   


    /*****
     * Login endpoint - authenticates the user and returns a JWT token in an HTTP-only cookie. The token will be used for subsequent requests to protected endpoints.
     * @param body - contains the email and password for authentication
     * @param response - used to set the JWT cookie on successful login
     * @return - returns the user's email and display name on successful login, or an error
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String email = body.get("email");
        String password = body.get("password");
        try {
            User user = authService.login(email, password);

            return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "displayName", user.getUserProfile().getDisplayName()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /*****
     * Profile endpoint - will return the user's profile information based on the JWT token.
     * @param token - the JWT token extracted from the HTTP-only cookie
     * @return - returns the user's email and display name if the token is valid, or an error if the token is invalid or expired
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@CookieValue(name = "jwt") String token) {
        return ResponseEntity.ok(Map.of("message", "This endpoint will return the user's profile information based on the JWT token in a future implementation."));
    }


    /*****
     * Logout endpoint - will clear the JWT cookie on the client side. 
     * @param response - used to clear the JWT cookie by setting its max age to 0
     * @return - returns a success message indicating the user has been logged out successfully
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
