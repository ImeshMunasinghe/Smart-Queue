package com.smartqueue.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents a staff user account (Operator or Admin).
 * Citizen data is intentionally NOT stored here — citizens are identified only by NIC hash.
 */
@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    /** BCrypt-hashed password. Raw password is never stored. */
    @Column(nullable = false)
    private String passwordHash;

    /**
     * Role string stored as granted authority — e.g. "ROLE_OPERATOR" or "ROLE_ADMIN".
     */
    @Column(nullable = false, length = 30)
    private String role;

    public AppUser() {}

    public AppUser(UUID id, String username, String passwordHash, String role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
