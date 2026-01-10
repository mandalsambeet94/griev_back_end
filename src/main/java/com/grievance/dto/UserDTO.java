package com.grievance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.grievance.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact must be 10 digits")
    private String contact;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Role is required")
    private String role;

    private Boolean isActive = true;

    private String blockAssigned;
    private String gpAssigned;

    // For password update
    private String currentPassword;
    private String newPassword;

    public User toEntity() {
        User user = new User();
        user.setName(this.name);
        user.setContact(this.contact);
        user.setRole(User.Role.valueOf(this.role.toUpperCase()));
        user.setIsActive(this.isActive);
        user.setBlockAssigned(this.blockAssigned);
        user.setGpAssigned(this.gpAssigned);
        return user;
    }

    public static UserDTO fromEntity(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setContact(user.getContact());
        dto.setRole(user.getRole().name());
        dto.setIsActive(user.getIsActive());
        dto.setBlockAssigned(user.getBlockAssigned());
        dto.setGpAssigned(user.getGpAssigned());
        return dto;
    }
}