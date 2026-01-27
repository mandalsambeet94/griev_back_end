package com.grievance.controller;

import com.grievance.dto.AuthRequest;
import com.grievance.dto.AuthResponse;
import com.grievance.dto.UserDTO;
import com.grievance.service.AuthService;
import com.grievance.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;


    @GetMapping("/hello")
    public String hello() {
        return "hello world";
    }

    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody UserDTO userDTO) {
        AuthResponse response = authService.register(userDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user details")
    public ResponseEntity<UserDTO> getCurrentUser() {
        UserDTO user = userService.getCurrentUserDetails();
        return ResponseEntity.ok(user);
    }
}