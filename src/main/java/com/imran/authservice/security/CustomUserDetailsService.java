package com.imran.authservice.security;

import com.imran.authservice.model.User;
import com.imran.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user by username/email: {}", username);

        // Try email first
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> {
                    log.info("User not found by email, trying username: {}", username);
                    return userRepository.findByUsername(username)
                            .orElseThrow(() -> {
                                log.error("User not found with email/username: {}", username);
                                return new UsernameNotFoundException("User not found with email/username: " + username);
                            });
                });

        log.info("User found: ID={}, Email={}, Status={}, Enabled={}",
                user.getId(), user.getEmail(), user.getStatus(), user.isEnabled());

        return user;
    }

    @Transactional
    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("Username not found with email: " + userId));
    }
}






