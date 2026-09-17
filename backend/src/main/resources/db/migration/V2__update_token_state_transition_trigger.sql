-- V2: Allow transitions from SERVING to NO_SHOW, SKIPPED, CANCELLED
CREATE OR REPLACE FUNCTION trg_fn_enforce_token_state_transition()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.state = NEW.state THEN
        RETURN NEW;
    END IF;

    IF (OLD.state = 'WAITING' AND NEW.state IN ('CALLED', 'CANCELLED')) OR
       (OLD.state = 'CALLED' AND NEW.state IN ('SERVING', 'NO_SHOW', 'SKIPPED', 'CANCELLED')) OR
       (OLD.state = 'SKIPPED' AND NEW.state IN ('CALLED', 'CANCELLED')) OR
       (OLD.state = 'SERVING' AND NEW.state IN ('COMPLETED', 'NO_SHOW', 'SKIPPED', 'CANCELLED')) THEN
        RETURN NEW;
    ELSE
        RAISE EXCEPTION 'ILLEGAL_STATE_TRANSITION: Invalid token transition from % to % for token id %', 
            OLD.state, NEW.state, OLD.id;
    END IF;
END;
$$ LANGUAGE plpgsql;
