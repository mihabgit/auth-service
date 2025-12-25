package com.imran.authservice.service;

import com.imran.authservice.config.JwtConfig;
import com.imran.authservice.dto.AuthRequest;
import com.imran.authservice.dto.AuthResponse;
import com.imran.authservice.dto.RegisterRequest;
import com.imran.authservice.dto.UserDto;
import com.imran.authservice.enums.UserStatus;
import com.imran.authservice.exception.InvalidTokenException;
import com.imran.authservice.exception.ResourceAlreadyExistsException;
import com.imran.authservice.model.RefreshToken;
import com.imran.authservice.model.User;
import com.imran.authservice.repository.RefreshTokenRepository;
import com.imran.authservice.repository.UserRepository;
import com.imran.authservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    @Value("${security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.lock-time-minutes:15}")
    private int lockTimeMinutes;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already in use");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username already taken");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .emailVerificationToken(generateEmailVerificationToken())
                .emailVerificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);

        // Send verification email
        // TODO

        return AuthResponse.builder()
                .message("Registration successful. Please verify your email.")
                .build();
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new LockedException("Account is locked. Try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Reset failed attempts on successful login
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = createRefreshToken(user, ipAddress, userAgent).getToken();

            UserDto userDto = mapToUserDto(user);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtConfig.getAccessTokenExpiration())
                    .user(userDto)
                    .build();
        } catch (BadCredentialsException e) {
            // Increment failed attempts
            int failedAttempts = user.getFailedLoginAttempts() == null ? 1 : user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            if (failedAttempts >= maxFailedAttempts) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockTimeMinutes));
                log.warn("Account locked for user: {}", user.getEmail());
            }

            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Transactional
    public String logout(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        storedToken.setRevoked(true);
        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);
        return "Successfully logged out.";
    }

    private RefreshToken createRefreshToken(User user, String ipAddress, String userAgent) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(jwtTokenProvider.generateRefreshToken(user.getId()))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private String generateEmailVerificationToken() {
        return UUID.randomUUID().toString();
    }

    private String generatePasswordResetToken() {
        return UUID.randomUUID().toString();
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus())
                .role(user.getRole())
                .lastLogin(user.getLastLogin())
                .mfaEnabled(user.isMfaEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

}











