package com.imran.authservice.service;

import com.imran.authservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    // Blacklist a token until it expires
    public void blacklistToken(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            Date now = new Date();

            // Calculate time until token expires
            long ttl = expiration.getTime() - now.getTime();

            if (ttl > 0) {
                // Store token in Redis with TTL equal to remaining token validity
                String key = BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttl));
                log.debug("Token blacklisted: will expire in {} ms", ttl);
            } else  {
                log.debug("Token already expired: no need to blacklist");
            }
        } catch (Exception e) {
            log.error("Error while blacklisting token: {}", e.getMessage());
        }
    }

    // Check if token is blacklisted
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    // Blacklist all tokens for a user
    public void blacklistAllUserTokens(String userId) {
        // This requires storing user-token mapping in Redis
        // Implementation depends on your token storage strategy
    }


    // Clear all blacklisted tokens (for cleanup)
    public void clearBlacklist() {
        // Use with caution - only for testing or maintenance
        redisTemplate.delete(redisTemplate.keys(BLACKLIST_PREFIX + "*"));
    }

}
