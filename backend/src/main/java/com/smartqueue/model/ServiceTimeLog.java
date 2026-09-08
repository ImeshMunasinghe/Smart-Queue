package com.smartqueue.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_time_log")
public class ServiceTimeLog implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "counter_id", nullable = false)
    private UUID counterId;

    @Column(name = "service_type_id", nullable = false)
    private UUID serviceTypeId;

    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "operator_id")
    private UUID operatorId;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private OffsetDateTime endedAt;

    @Transient
    private boolean isNew = true;

    public ServiceTimeLog() {
        this.id = UUID.randomUUID();
    }

    public ServiceTimeLog(UUID id, UUID officeId, UUID counterId, UUID serviceTypeId, UUID tokenId,
                          UUID operatorId, int durationSeconds, OffsetDateTime startedAt, OffsetDateTime endedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.officeId = officeId;
        this.counterId = counterId;
        this.serviceTypeId = serviceTypeId;
        this.tokenId = tokenId;
        this.operatorId = operatorId;
        this.durationSeconds = durationSeconds;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
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

    public UUID getCounterId() { return counterId; }
    public void setCounterId(UUID counterId) { this.counterId = counterId; }

    public UUID getServiceTypeId() { return serviceTypeId; }
    public void setServiceTypeId(UUID serviceTypeId) { this.serviceTypeId = serviceTypeId; }

    public UUID getTokenId() { return tokenId; }
    public void setTokenId(UUID tokenId) { this.tokenId = tokenId; }

    public UUID getOperatorId() { return operatorId; }
    public void setOperatorId(UUID operatorId) { this.operatorId = operatorId; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
}
