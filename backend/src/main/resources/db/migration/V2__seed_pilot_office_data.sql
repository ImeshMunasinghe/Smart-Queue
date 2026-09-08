-- ==============================================================================
-- Migration: V2__seed_pilot_office_data.sql
-- Seeds default pilot office (Colombo DS Office / Pilot Clinic),
-- service types, initial counters, and today's slot capacities.
-- ==============================================================================

-- 1. Pilot Office
INSERT INTO offices (id, name, code, timezone, address, active)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Colombo Divisional Secretariat (Pilot Office)',
    'DS-COLOMBO-PILOT',
    'Asia/Colombo',
    'Dam Street, Colombo 12, Sri Lanka',
    TRUE
) ON CONFLICT (code) DO NOTHING;

-- 2. Pilot Service Types
INSERT INTO service_types (id, office_id, code, name, description, default_duration_minutes, min_service_time_seconds, max_service_time_seconds, active)
VALUES 
(
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'NIC_RENEWAL',
    'National Identity Card (NIC) Services',
    'New issuance, renewal, and replacement of NICs',
    12,
    180,
    1800,
    TRUE
),
(
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'GRAMA_CERT',
    'Grama Niladhari Character / Residence Certificate',
    'Verification of residence and character certificates',
    8,
    120,
    900,
    TRUE
),
(
    'b0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'OPD_CONSULT',
    'General OPD Medical Consultation',
    'Outpatient department preliminary clinical screening',
    15,
    300,
    2400,
    TRUE
) ON CONFLICT (office_id, code) DO NOTHING;

-- 3. Pilot Counters (Restricted and General Eligibility)
INSERT INTO counters (id, office_id, counter_number, name, status, eligible_service_types, version)
VALUES 
(
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'C1',
    'Counter 1 (NIC Priority)',
    'ONLINE',
    '["b0000000-0000-0000-0000-000000000001"]'::jsonb,
    0
),
(
    'c0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'C2',
    'Counter 2 (Grama Cert & General)',
    'ONLINE',
    '["b0000000-0000-0000-0000-000000000001", "b0000000-0000-0000-0000-000000000002"]'::jsonb,
    0
),
(
    'c0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'C3',
    'Counter 3 (OPD Consultation)',
    'ONLINE',
    '["b0000000-0000-0000-0000-000000000003"]'::jsonb,
    0
) ON CONFLICT (office_id, counter_number) DO NOTHING;

-- 4. Initial Slot Capacities for Today and Upcoming Days
INSERT INTO slot_capacity (id, office_id, service_type_id, session_date, start_time, end_time, raw_capacity, computed_limit, issued_count, active_waiting_count, risk_threshold, version)
VALUES 
(
    'd0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    CURRENT_DATE,
    '08:30:00',
    '12:30:00',
    40,
    46, -- 15% overbooked based on 10% risk threshold
    0,
    0,
    0.100,
    0
),
(
    'd0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000002',
    CURRENT_DATE,
    '08:30:00',
    '12:30:00',
    50,
    58,
    0,
    0,
    0.100,
    0
),
(
    'd0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000003',
    CURRENT_DATE,
    '08:00:00',
    '13:00:00',
    60,
    70,
    0,
    0,
    0.100,
    0
) ON CONFLICT (office_id, service_type_id, session_date, start_time) DO NOTHING;
