package com.smartqueue.security;

import com.smartqueue.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Spring Security configuration.
 *
 * Access rules:
 *   PUBLIC       — Citizen portal (POST /api/v1/tokens), SSE stream, token status, public counters list,
 *                  office list, SMS webhook, actuator health, TV display, and auth login endpoint.
 *   ROLE_OPERATOR — All POST/PUT actions on /api/v1/operator/** (call-next, serve, complete, skip, etc.)
 *   ROLE_ADMIN   — Admin slot overrides, analytics, and slot lookups via /api/v1/admin/**
 *
 * Session management is STATELESS — JWTs carry all auth state.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AppUserRepository appUserRepository;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          RateLimitFilter rateLimitFilter,
                          AppUserRepository appUserRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.appUserRepository = appUserRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF not needed for a stateless JWT-authenticated REST API
            .csrf(AbstractHttpConfigurer::disable)
            // No server-side sessions
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── Auth endpoint ─────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                // ── Citizen public endpoints ──────────────────────────────────
                // Citizens issue tokens, track via SSE, and cancel — all unauthenticated
                .requestMatchers(HttpMethod.POST, "/api/v1/tokens").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/tokens/*/status").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/tokens/*/stream").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/tokens/*/cancel").permitAll()

                // ── SMS webhook (telco ingress, no user session) ──────────────
                .requestMatchers("/api/v1/sms/**", "/api/v1/webhooks/sms/**").permitAll()

                // ── Office & counter discovery (used by citizen portal + TV display) ──
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/offices").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/offices/*/service-types").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/operator/offices/*/counters", "/api/v1/operator/counters/*").permitAll()

                // ── Actuator health check ────────────────────────────────────
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // ── Operator-protected actions (accessible by OPERATOR or ADMIN) ─────
                .requestMatchers(HttpMethod.POST, "/api/v1/operator/**").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/v1/operator/**").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/v1/operator/counters/*/queue").hasAnyRole("OPERATOR", "ADMIN")

                // ── Admin-protected actions ───────────────────────────────────
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // ── Deny everything else by default ──────────────────────────
                .anyRequest().authenticated()
            )
            // Register rate limit filter first (before JWT auth)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // Register JWT filter before Spring's username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> appUserRepository.findByUsername(username)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority(user.getRole()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Staff user not found: " + username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService uds) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
