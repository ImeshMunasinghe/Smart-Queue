package com.smartqueue.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "slot_capacity", uniqueConstraints = {
    @UniqueConstraint(name = "uq_office_slot", columnNames = {"office_id", "service_type_id", "session_date", "start_time"})
})
public class SlotCapacity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "service_type_id", nullable = false)
    private UUID serviceTypeId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "raw_capacity", nullable = false)
    private int rawCapacity;

    @Column(name = "computed_limit", nullable = false)
    private int computedLimit;

    @Column(name = "manual_override_limit")
    private Integer manualOverrideLimit;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount = 0;

    @Column(name = "active_waiting_count", nullable = false)
    private int activeWaitingCount = 0;

    @Column(name = "risk_threshold", nullable = false, precision = 4, scale = 3)
    private BigDecimal riskThreshold = new BigDecimal("0.100");

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Transient
    private boolean isNew = true;

    public SlotCapacity() {
        this.id = UUID.randomUUID();
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

    public int getEffectiveMaxCapacity() {
        if (manualOverrideLimit != null) {
            return manualOverrideLimit;
        }
        return computedLimit > 0 ? computedLimit : rawCapacity;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOfficeId() { return officeId; }
    public void setOfficeId(UUID officeId) { this.officeId = officeId; }

    public UUID getServiceTypeId() { return serviceTypeId; }
    public void setServiceTypeId(UUID serviceTypeId) { this.serviceTypeId = serviceTypeId; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getRawCapacity() { return rawCapacity; }
    public void setRawCapacity(int rawCapacity) { this.rawCapacity = rawCapacity; }

    public int getComputedLimit() { return computedLimit; }
    public void setComputedLimit(int computedLimit) { this.computedLimit = computedLimit; }

    public Integer getManualOverrideLimit() { return manualOverrideLimit; }
    public void setManualOverrideLimit(Integer manualOverrideLimit) { this.manualOverrideLimit = manualOverrideLimit; }

    public int getIssuedCount() { return issuedCount; }
    public void setIssuedCount(int issuedCount) { this.issuedCount = issuedCount; }

    public int getActiveWaitingCount() { return activeWaitingCount; }
    public void setActiveWaitingCount(int activeWaitingCount) { this.activeWaitingCount = activeWaitingCount; }

    public BigDecimal getRiskThreshold() { return riskThreshold; }
    public void setRiskThreshold(BigDecimal riskThreshold) { this.riskThreshold = riskThreshold; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
