package com.pokemon.inventory.controller;

import com.pokemon.inventory.dto.AuthResponse;
import com.pokemon.inventory.dto.LoginRequest;
import com.pokemon.inventory.dto.RegisterRequest;
import com.pokemon.inventory.model.User;
import com.pokemon.inventory.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpSession session) {
        String username = request.getUsername().trim().toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (username.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username must be at least 2 characters (letters, numbers, hyphens)"));
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 4 characters"));
        }
        if (userRepo.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already taken"));
        }

        User user = new User();
        user.setUsername(username);
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName().trim() : username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepo.save(user);

        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(user.getUsername(), user.getDisplayName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        String username = request.getUsername().trim().toLowerCase();
        return userRepo.findByUsername(username)
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("username", user.getUsername());
                    return ResponseEntity.ok((Object) new AuthResponse(user.getUsername(), user.getDisplayName()));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        return userRepo.findById(userId)
                .map(user -> ResponseEntity.ok((Object) new AuthResponse(user.getUsername(), user.getDisplayName())))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found")));
    }
}
