package com.smartqueue.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "counters")
public class Counter implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "counter_number", nullable = false, length = 10)
    private String counterNumber;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CounterStatus status = CounterStatus.OFFLINE;

    @Column(name = "eligible_service_types", columnDefinition = "TEXT")
    private String eligibleServiceTypesJson = "[]";

    @Column(name = "current_operator_id")
    private UUID currentOperatorId;

    @Column(name = "current_token_id")
    private UUID currentTokenId;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Transient
    private boolean isNew = true;

    public Counter() {
        this.id = UUID.randomUUID();
    }

    public Counter(UUID id, UUID officeId, String counterNumber, String name, CounterStatus status, String eligibleServiceTypesJson) {
        this.id = id != null ? id : UUID.randomUUID();
        this.officeId = officeId;
        this.counterNumber = counterNumber;
        this.name = name;
        this.status = status;
        this.eligibleServiceTypesJson = eligibleServiceTypesJson;
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

    public String getCounterNumber() { return counterNumber; }
    public void setCounterNumber(String counterNumber) { this.counterNumber = counterNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public CounterStatus getStatus() { return status; }
    public void setStatus(CounterStatus status) { this.status = status; }

    public String getEligibleServiceTypesJson() { return eligibleServiceTypesJson; }
    public void setEligibleServiceTypesJson(String eligibleServiceTypesJson) { this.eligibleServiceTypesJson = eligibleServiceTypesJson; }

    public UUID getCurrentOperatorId() { return currentOperatorId; }
    public void setCurrentOperatorId(UUID currentOperatorId) { this.currentOperatorId = currentOperatorId; }

    public UUID getCurrentTokenId() { return currentTokenId; }
    public void setCurrentTokenId(UUID currentTokenId) { this.currentTokenId = currentTokenId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
