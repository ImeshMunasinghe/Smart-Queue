package com.smartqueue.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis sliding-window rate limiter applied to POST /api/v1/tokens (token issuance).
 *
 * Algorithm: INCR + EXPIRE on key "rate:ip:<clientIp>"
 *   - First request in a window: INCR sets count to 1, EXPIRE sets TTL.
 *   - Subsequent requests: INCR increments existing count.
 *   - When count > limit → HTTP 429 Too Many Requests.
 *
 * Configured via application.properties:
 *   rate.limit.requests-per-window=10
 *   rate.limit.window-seconds=60
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final int requestsPerWindow;
    private final long windowSeconds;

    public RateLimitFilter(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${rate.limit.requests-per-window:10}") int requestsPerWindow,
            @Value("${rate.limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.requestsPerWindow = requestsPerWindow;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only rate-limit POST /api/v1/tokens (citizen token issuance)
        return !(request.getMethod().equalsIgnoreCase("POST")
                && request.getRequestURI().equals("/api/v1/tokens"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String key = "rate:ip:" + clientIp;

        try {
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1) {
                // First request in this window — set the TTL
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }

            if (count != null && count > requestsPerWindow) {
                log.warn("Rate limit exceeded for IP {} — count {} > limit {}", clientIp, count, requestsPerWindow);
                sendRateLimitResponse(response, clientIp);
                return;
            }

        } catch (Exception redisException) {
            // Redis unavailable — fail open (allow request) to preserve availability
            log.warn("Redis rate limiter unavailable, failing open: {}", redisException.getMessage());
        }

        chain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletResponse response, String clientIp) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = objectMapper.writeValueAsString(Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", String.format(
                        "Token issuance rate limit exceeded. Max %d requests per %d seconds per IP. Please wait before trying again.",
                        requestsPerWindow, windowSeconds)
        ));
        response.getWriter().write(body);
    }

    /**
     * Resolves the real client IP, respecting X-Forwarded-For headers from reverse proxies.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be a comma-separated list — take the first (original client)
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
