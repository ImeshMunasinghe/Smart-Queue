package com.smartqueue.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens")
public class Token implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "token_number", nullable = false, length = 20)
    private String tokenNumber;

    @Column(name = "service_type_id", nullable = false)
    private UUID serviceTypeId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "citizen_reference_hash", nullable = false, length = 64)
    private String citizenReferenceHash;

    @Column(name = "citizen_phone_masked", nullable = false, length = 20)
    private String citizenPhoneMasked;

    @Column(nullable = false, length = 20)
    private String channel = "WEB"; // WEB, USSD, SMS, OPERATOR

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenState state = TokenState.WAITING;

    @Column(name = "assigned_counter_id")
    private UUID assignedCounterId;

    @Column(nullable = false)
    private int priority = 0;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "estimated_call_time")
    private OffsetDateTime estimatedCallTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "called_at")
    private OffsetDateTime calledAt;

    @Column(name = "served_at")
    private OffsetDateTime servedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Transient
    private boolean isNew = true;

    public Token() {
        this.id = UUID.randomUUID();
    }

    public Token(UUID id, UUID officeId, String tokenNumber, UUID serviceTypeId, UUID slotId,
                 String citizenReferenceHash, String citizenPhoneMasked, String channel, String idempotencyKey) {
        this.id = id != null ? id : UUID.randomUUID();
        this.officeId = officeId;
        this.tokenNumber = tokenNumber;
        this.serviceTypeId = serviceTypeId;
        this.slotId = slotId;
        this.citizenReferenceHash = citizenReferenceHash;
        this.citizenPhoneMasked = citizenPhoneMasked;
        this.channel = channel;
        this.idempotencyKey = idempotencyKey;
        this.state = TokenState.WAITING;
        this.version = 0L;
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

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    public UUID getServiceTypeId() { return serviceTypeId; }
    public void setServiceTypeId(UUID serviceTypeId) { this.serviceTypeId = serviceTypeId; }

    public UUID getSlotId() { return slotId; }
    public void setSlotId(UUID slotId) { this.slotId = slotId; }

    public String getCitizenReferenceHash() { return citizenReferenceHash; }
    public void setCitizenReferenceHash(String citizenReferenceHash) { this.citizenReferenceHash = citizenReferenceHash; }

    public String getCitizenPhoneMasked() { return citizenPhoneMasked; }
    public void setCitizenPhoneMasked(String citizenPhoneMasked) { this.citizenPhoneMasked = citizenPhoneMasked; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public TokenState getState() { return state; }
    public void setState(TokenState state) { this.state = state; }

    public UUID getAssignedCounterId() { return assignedCounterId; }
    public void setAssignedCounterId(UUID assignedCounterId) { this.assignedCounterId = assignedCounterId; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public OffsetDateTime getEstimatedCallTime() { return estimatedCallTime; }
    public void setEstimatedCallTime(OffsetDateTime estimatedCallTime) { this.estimatedCallTime = estimatedCallTime; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCalledAt() { return calledAt; }
    public void setCalledAt(OffsetDateTime calledAt) { this.calledAt = calledAt; }

    public OffsetDateTime getServedAt() { return servedAt; }
    public void setServedAt(OffsetDateTime servedAt) { this.servedAt = servedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
