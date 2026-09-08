package com.smartqueue.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offices")
public class Office implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Colombo";

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Transient
    private boolean isNew = true;

    public Office() {
        this.id = UUID.randomUUID();
    }

    public Office(UUID id, String name, String code, String timezone, String address) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.code = code;
        this.timezone = timezone;
        this.address = address;
        this.active = true;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
