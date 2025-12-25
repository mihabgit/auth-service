package com.imran.authservice.dto;

import com.imran.authservice.enums.Role;
import com.imran.authservice.enums.UserStatus;
import com.imran.authservice.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private Role role;
    private LocalDateTime lastLogin;
    private boolean mfaEnabled;
    private LocalDateTime createdAt;
}
















