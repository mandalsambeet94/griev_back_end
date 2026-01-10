package com.grievance.dto;

import com.grievance.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String name;
    private String contact;
    private User.Role role;
    private Boolean isActive;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.userId = user.getId();
        this.name = user.getName();
        this.contact = user.getContact();
        this.role = user.getRole();
        this.isActive = user.getIsActive();
    }
}