package com.grievance.service;

import com.grievance.dto.UserDTO;
import com.grievance.entity.User;
import com.grievance.exception.ResourceNotFoundException;
import com.grievance.exception.UnauthorizedException;
import com.grievance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserDTO getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDTO.fromEntity(user);
    }

    public UserDTO getCurrentUserDetails() {
        User user = authService.getCurrentUser();
        return UserDTO.fromEntity(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getAgents() {
        return userRepository.findByRole(User.Role.AGENT).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getActiveAgents() {
        return userRepository.findByRoleAndIsActiveTrue(User.Role.AGENT).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check permissions
        User currentUser = authService.getCurrentUser();
        if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN) &&
                !currentUser.getId().equals(userId)) {
            throw new UnauthorizedException("You can only update your own profile");
        }

        // Update fields
        if (userDTO.getName() != null) {
            user.setName(userDTO.getName());
        }

        if (userDTO.getBlockAssigned() != null) {
            user.setBlockAssigned(userDTO.getBlockAssigned());
        }

        if (userDTO.getGpAssigned() != null) {
            user.setGpAssigned(userDTO.getGpAssigned());
        }

        // Only admin can change role
        if (userDTO.getRole() != null &&
                (currentUser.getRole().equals(User.Role.ADMIN) ||
                        currentUser.getRole().equals(User.Role.SUPER_ADMIN))) {
            user.setRole(User.Role.valueOf(userDTO.getRole().toUpperCase()));
        }

        // Update password if provided
        if (userDTO.getCurrentPassword() != null && userDTO.getNewPassword() != null) {
            if (!passwordEncoder.matches(userDTO.getCurrentPassword(), user.getPassword())) {
                throw new UnauthorizedException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(userDTO.getNewPassword()));
        }

        User updatedUser = userRepository.save(user);
        return UserDTO.fromEntity(updatedUser);
    }

    @Transactional
    public UserDTO activateDeactivateUser(Long userId, boolean activate) {
        User currentUser = authService.getCurrentUser();

        // Only admin can activate/deactivate
        if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new UnauthorizedException("Only admin can activate/deactivate users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Cannot deactivate yourself
        if (currentUser.getId().equals(userId)) {
            throw new UnauthorizedException("Cannot deactivate your own account");
        }

        user.setIsActive(activate);
        User updatedUser = userRepository.save(user);

        return UserDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = authService.getCurrentUser();

        // Only admin can delete
        if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new UnauthorizedException("Only admin can delete users");
        }

        // Cannot delete yourself
        if (currentUser.getId().equals(userId)) {
            throw new UnauthorizedException("Cannot delete your own account");
        }

        userRepository.deleteById(userId);
    }
}