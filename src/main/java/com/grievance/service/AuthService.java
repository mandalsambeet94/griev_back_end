package com.grievance.service;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.grievance.dto.AuthRequest;
import com.grievance.dto.AuthResponse;
import com.grievance.dto.UserDTO;
import com.grievance.entity.User;
import com.grievance.exception.UnauthorizedException;
import com.grievance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(UserDTO userDTO) {
        // Check if user already exists
        if (userRepository.existsByContact(userDTO.getContact())) {
            throw new RuntimeException("User with this contact already exists");
        }

        User user = userDTO.toEntity();
        System.out.println("enum val: "+ user.getRole().getClass());
        user.setPassword(passwordEncoder.encode(userDTO.getNewPassword()));

        User savedUser = userRepository.save(user);

        // Generate token1
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(token, savedUser);
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getContact(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String contact = authentication.getName();
        return userRepository.findByContact(contact)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}