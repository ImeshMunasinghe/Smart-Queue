-- ==============================================================================
-- Migration: V1__init_smart_queue_schema.sql
-- Smart Queue Token System Schema (Pilot Seeded for Scale)
-- Enforces office_id partitioning, state machine trigger validation, & audit logs
-- ==============================================================================

-- Enable UUID extension if available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Offices
CREATE TABLE offices (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Colombo',
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Service Types
CREATE TABLE service_types (
    id UUID PRIMARY KEY,
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    default_duration_minutes INT NOT NULL DEFAULT 15,
    min_service_time_seconds INT NOT NULL DEFAULT 180,
    max_service_time_seconds INT NOT NULL DEFAULT 3600,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_office_service_code UNIQUE (office_id, code)
);

CREATE INDEX idx_service_types_office ON service_types (office_id);

-- 3. Counters
CREATE TABLE counters (
    id UUID PRIMARY KEY,
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    counter_number VARCHAR(10) NOT NULL,
    name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE', -- ONLINE, BUSY, PAUSED, OFFLINE
    eligible_service_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    current_operator_id UUID,
    current_token_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_office_counter_number UNIQUE (office_id, counter_number)
);

CREATE INDEX idx_counters_office_status ON counters (office_id, status);

-- 4. Slot Capacity & Overbooking Registry
CREATE TABLE slot_capacity (
    id UUID PRIMARY KEY,
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    service_type_id UUID NOT NULL REFERENCES service_types(id) ON DELETE CASCADE,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    raw_capacity INT NOT NULL,
    computed_limit INT NOT NULL,
    manual_override_limit INT,
    issued_count INT NOT NULL DEFAULT 0,
    active_waiting_count INT NOT NULL DEFAULT 0,
    risk_threshold NUMERIC(4,3) NOT NULL DEFAULT 0.100,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_office_slot UNIQUE (office_id, service_type_id, session_date, start_time)
);

CREATE INDEX idx_slot_capacity_lookup ON slot_capacity (office_id, service_type_id, session_date);

-- 5. Tokens
CREATE TABLE tokens (
    id UUID PRIMARY KEY,
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    token_number VARCHAR(20) NOT NULL,
    service_type_id UUID NOT NULL REFERENCES service_types(id) ON DELETE RESTRICT,
    slot_id UUID NOT NULL REFERENCES slot_capacity(id) ON DELETE RESTRICT,
    citizen_reference_hash VARCHAR(64) NOT NULL, -- SHA-256 of NIC
    citizen_phone_masked VARCHAR(20) NOT NULL,   -- e.g. +94****1234
    channel VARCHAR(20) NOT NULL DEFAULT 'WEB',  -- WEB, USSD, SMS, OPERATOR
    state VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    assigned_counter_id UUID REFERENCES counters(id) ON DELETE SET NULL,
    priority INT NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(128),
    estimated_call_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    called_at TIMESTAMP WITH TIME ZONE,
    served_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_token_state CHECK (
        state IN ('WAITING', 'CALLED', 'SERVING', 'COMPLETED', 'NO_SHOW', 'SKIPPED', 'CANCELLED')
    )
);

CREATE INDEX idx_tokens_active_queue ON tokens (office_id, service_type_id, state, priority DESC, created_at ASC);
CREATE INDEX idx_tokens_slot ON tokens (slot_id);
CREATE INDEX idx_tokens_assigned_counter ON tokens (assigned_counter_id, state);
CREATE INDEX idx_tokens_citizen_active ON tokens (citizen_reference_hash, service_type_id, state);
CREATE UNIQUE INDEX uq_tokens_idempotency ON tokens (office_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 6. Token State Transitions Audit (Append-only)
CREATE TABLE token_state_transitions (
    id BIGSERIAL PRIMARY KEY,
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    token_id UUID NOT NULL REFERENCES tokens(id) ON DELETE CASCADE,
    from_state VARCHAR(20) NOT NULL,
    to_state VARCHAR(20) NOT NULL,
    triggered_by VARCHAR(50) NOT NULL, -- CITIZEN, OPERATOR, TIMEOUT_WORKER, ADMIN
    operator_id UUID,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_state_transitions_token ON token_state_transitions (token_id, created_at);

-- 7. Service Time Log (ML Training Data for Wait-Time Regression)
CREATE TABLE service_time_log (
    id UUID PRIMARY KEY,
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    counter_id UUID NOT NULL REFERENCES counters(id) ON DELETE CASCADE,
    service_type_id UUID NOT NULL REFERENCES service_types(id) ON DELETE CASCADE,
    token_id UUID NOT NULL REFERENCES tokens(id) ON DELETE CASCADE,
    operator_id UUID,
    duration_seconds INT NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_service_time_log_ml ON service_time_log (office_id, service_type_id, started_at);

-- 8. No-Show History (ML Training Data for No-Show Classification)
CREATE TABLE no_show_history (
    id UUID PRIMARY KEY,
    office_id UUID NOT NULL REFERENCES offices(id) ON DELETE CASCADE,
    citizen_reference_hash VARCHAR(64) NOT NULL,
    service_type_id UUID NOT NULL REFERENCES service_types(id) ON DELETE CASCADE,
    session_date DATE NOT NULL,
    slot_time TIME NOT NULL,
    lead_time_minutes INT NOT NULL,
    showed_up BOOLEAN NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_no_show_citizen ON no_show_history (citizen_reference_hash);
CREATE INDEX idx_no_show_service ON no_show_history (office_id, service_type_id, session_date);

-- 9. Audit Logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    office_id UUID REFERENCES offices(id) ON DELETE CASCADE,
    user_id UUID,
    user_role VARCHAR(30) NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_resource VARCHAR(50) NOT NULL,
    resource_id UUID,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_office ON audit_logs (office_id, created_at);

-- 10. Database-level State Transition Trigger (FR-3.3 Enforcement)
CREATE OR REPLACE FUNCTION trg_fn_enforce_token_state_transition()
RETURNS TRIGGER AS $$
BEGIN
    -- Only check when state actually changes
    IF OLD.state = NEW.state THEN
        RETURN NEW;
    END IF;

    -- Valid Transitions Matrix:
    -- WAITING   -> CALLED, CANCELLED
    -- CALLED    -> SERVING, NO_SHOW, SKIPPED, CANCELLED
    -- SKIPPED   -> CALLED, CANCELLED
    -- SERVING   -> COMPLETED
    IF (OLD.state = 'WAITING' AND NEW.state IN ('CALLED', 'CANCELLED')) OR
       (OLD.state = 'CALLED' AND NEW.state IN ('SERVING', 'NO_SHOW', 'SKIPPED', 'CANCELLED')) OR
       (OLD.state = 'SKIPPED' AND NEW.state IN ('CALLED', 'CANCELLED')) OR
       (OLD.state = 'SERVING' AND NEW.state = 'COMPLETED') THEN
        RETURN NEW;
    ELSE
        RAISE EXCEPTION 'ILLEGAL_STATE_TRANSITION: Invalid token transition from % to % for token id %', 
            OLD.state, NEW.state, OLD.id;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_token_state_machine
BEFORE UPDATE OF state ON tokens
FOR EACH ROW
EXECUTE FUNCTION trg_fn_enforce_token_state_transition();
