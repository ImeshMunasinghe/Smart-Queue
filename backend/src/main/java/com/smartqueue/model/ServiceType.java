package com.smartqueue.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_types")
public class ServiceType implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_duration_minutes", nullable = false)
    private int defaultDurationMinutes = 15;

    @Column(name = "min_service_time_seconds", nullable = false)
    private int minServiceTimeSeconds = 180;

    @Column(name = "max_service_time_seconds", nullable = false)
    private int maxServiceTimeSeconds = 3600;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Transient
    private boolean isNew = true;

    public ServiceType() {
        this.id = UUID.randomUUID();
    }

    public ServiceType(UUID id, UUID officeId, String code, String name, String description, int defaultDurationMinutes) {
        this.id = id != null ? id : UUID.randomUUID();
        this.officeId = officeId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.defaultDurationMinutes = defaultDurationMinutes;
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

    public UUID getOfficeId() { return officeId; }
    public void setOfficeId(UUID officeId) { this.officeId = officeId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public void setDefaultDurationMinutes(int defaultDurationMinutes) { this.defaultDurationMinutes = defaultDurationMinutes; }

    public int getMinServiceTimeSeconds() { return minServiceTimeSeconds; }
    public void setMinServiceTimeSeconds(int minServiceTimeSeconds) { this.minServiceTimeSeconds = minServiceTimeSeconds; }

    public int getMaxServiceTimeSeconds() { return maxServiceTimeSeconds; }
    public void setMaxServiceTimeSeconds(int maxServiceTimeSeconds) { this.maxServiceTimeSeconds = maxServiceTimeSeconds; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
