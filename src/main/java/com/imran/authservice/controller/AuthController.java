package com.imran.authservice.controller;

import com.imran.authservice.dto.AuthRequest;
import com.imran.authservice.dto.AuthResponse;
import com.imran.authservice.dto.RefreshTokenRequest;
import com.imran.authservice.dto.RegisterRequest;
import com.imran.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                              HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent =  servletRequest.getHeader("User-Agent");

        AuthResponse response = authService.authenticate(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequest request) {
        String response = authService.logout(request.getRefreshToken());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}














