package auth;

import org.springframework.web.bind.annotation.PostMapping;
@RestController
@RequestMapping("/auth")
public class AuthController {
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("There is already an account under this email.");
        }

        String salt = BCrypt.gensalt();
        String hash = BCrypt.hashpw(user.getPassword(), salt);

        User newUser = new User();
        newUser.setEmail(user.getEmail());
        newUser.setUsername(user.getUsername());
        newUser.setPasswordHash(hash);
        newUser.setPasswordSalt(salt);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(newUser);
        return ResponseEntity.ok().body("User registered successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        Optional<User> existingUserOpt = userRepository.findByEmail(user.getEmail());
        if (existingUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        }

        User existingUser = existingUserOpt.get();
        String hash = BCrypt.hashpw(user.getPassword(), existingUser.getPasswordSalt());
        if (!hash.equals(existingUser.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        }

        String token = jwtService.generateToken(existingUser);
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) jwtService.getExpiration() / 1000);
        response.addCookie(cookie);
        return ResponseEntity.ok().body(Map.of("token", token));
    }
}