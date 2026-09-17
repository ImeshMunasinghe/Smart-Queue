package com.smartqueue.security;

import com.smartqueue.model.AppUser;
import com.smartqueue.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seeds two hardcoded demo staff accounts on first boot if no users exist.
 *
 * Demo credentials:
 *   operator1 / operator123  →  ROLE_OPERATOR
 *   admin     / admin123     →  ROLE_ADMIN
 *
 * In production, replace with a proper user management flow.
 * Runs AFTER DataInitializer (Order 2).
 */
@Component
@Order(2)
public class StaffUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffUserSeeder.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffUserSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            log.info("Staff users already seeded. Skipping.");
            return;
        }

        log.info("Seeding demo staff user accounts...");

        AppUser operator = new AppUser(
                UUID.fromString("d0000000-0000-0000-0000-000000000001"),
                "operator1",
                passwordEncoder.encode("operator123"),
                "ROLE_OPERATOR"
        );

        AppUser admin = new AppUser(
                UUID.fromString("d0000000-0000-0000-0000-000000000002"),
                "admin",
                passwordEncoder.encode("admin123"),
                "ROLE_ADMIN"
        );

        appUserRepository.save(operator);
        appUserRepository.save(admin);

        log.info("Demo staff accounts created: operator1 (ROLE_OPERATOR), admin (ROLE_ADMIN)");
    }
}
