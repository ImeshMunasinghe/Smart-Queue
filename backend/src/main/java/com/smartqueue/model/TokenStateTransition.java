package com.smartqueue.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "token_state_transitions")
public class TokenStateTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "from_state", nullable = false, length = 20)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 20)
    private String toState;

    @Column(name = "triggered_by", nullable = false, length = 50)
    private String triggeredBy;

    @Column(name = "operator_id")
    private UUID operatorId;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TokenStateTransition() {}

    public TokenStateTransition(UUID officeId, UUID tokenId, String fromState, String toState,
                                String triggeredBy, UUID operatorId, String reason) {
        this.officeId = officeId;
        this.tokenId = tokenId;
        this.fromState = fromState;
        this.toState = toState;
        this.triggeredBy = triggeredBy;
        this.operatorId = operatorId;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public UUID getOfficeId() { return officeId; }
    public UUID getTokenId() { return tokenId; }
    public String getFromState() { return fromState; }
    public String getToState() { return toState; }
    public String getTriggeredBy() { return triggeredBy; }
    public UUID getOperatorId() { return operatorId; }
    public String getReason() { return reason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
