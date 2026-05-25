package com.grievance.service;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.grievance.dto.AuthRequest;
import com.grievance.dto.AuthResponse;
import com.grievance.dto.UserDTO;
import com.grievance.entity.User;
import com.grievance.exception.ResourceNotFoundException;
import com.grievance.exception.UnauthorizedException;
import com.grievance.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthResponse register(UserDTO userDTO) {
        // Check if user already exists
        if (userRepository.existsByContact(userDTO.getContact())) {
            throw new RuntimeException("User with this contact already exists");
        }

        User user = userDTO.toEntity();
        //System.out.println("enum val: "+ user.getRole().getClass());
        user.setPassword(passwordEncoder.encode(userDTO.getNewPassword()));

        User savedUser = userRepository.save(user);

        // Generate token1
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(token, savedUser);
    }

    /*public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getName(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }
        if (StringUtils.hasText(user.getRole().name()) && StringUtils.hasText(request.getRole())) {
            if (!user.getRole().name().equals(request.getRole())) {
                throw new UnauthorizedException("Role doesn't match.");
            }
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user);
    }*/

    public AuthResponse login(AuthRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getName(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        if (StringUtils.hasText(user.getRole().name()) && StringUtils.hasText(request.getRole())) {
            if (!user.getRole().name().equals(request.getRole())) {
                throw new UnauthorizedException("Role doesn't match.");
            }
        }

        // 🔥 NEW LOGIC STARTS HERE
        if ("AGENT".equalsIgnoreCase(user.getRole().name())) {

            if (user.getActiveToken() != null &&
                    user.getTokenExpiry() != null &&
                    user.getTokenExpiry().isAfter(LocalDateTime.now())) {

                throw new UnauthorizedException(
                        "You are already logged in on another device. Please logout first."
                );
            }
        }
        // 🔥 NEW LOGIC ENDS HERE

        String token = jwtService.generateToken(user);

        // 🔥 SAVE SESSION
        if (user.getRole().equals(User.Role.AGENT)) {
            user.setActiveToken(token);
            user.setTokenExpiry(jwtService.extractExpiration(token).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()); // you may need to implement this
            userRepository.save(user);
        }

        return new AuthResponse(token, user);
    }

    public ResponseEntity<String> logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok("Already logged out"); // idempotent
        }

        String jwt = authHeader.substring(7);

        try {

            String username = jwtService.extractUsername(jwt);

            User user = userRepository.findByName(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (user == null) {
                return ResponseEntity.ok("Already logged out");
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

            // ✅ Validate token
            if (!jwtService.validateToken(jwt, userDetails)) {
                return ResponseEntity.ok("Already logged out");
            }

            // ✅ Check session match (important for AGENT logic)
            if ("AGENT".equalsIgnoreCase(user.getRole().name())) {

                if (user.getActiveToken() != null &&
                        jwt.equals(user.getActiveToken())) {

                    user.setActiveToken(null);
                    user.setTokenExpiry(null);
                    userRepository.save(user);
                }
            }

            return ResponseEntity.ok("Logged out successfully");

        } catch (Exception ex) {
            return ResponseEntity.ok("Already logged out");
        }
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String name = authentication.getName();
        return userRepository.findByName(name)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}