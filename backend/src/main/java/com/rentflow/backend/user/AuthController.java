package com.rentflow.backend.user;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(
        UserService userService,
        AuthService authService,
        UserRepository userRepository
    ) {
        this.userService = userService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("fullName", user.getFullName());
        result.put("email", user.getEmail());
        result.put("role", user.getRole().name());
        return result;
    }
}