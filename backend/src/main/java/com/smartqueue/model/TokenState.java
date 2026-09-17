package com.smartqueue.model;

import java.util.Set;

public enum TokenState {
    WAITING,
    CALLED,
    SERVING,
    COMPLETED,
    NO_SHOW,
    SKIPPED,
    CANCELLED;

    public boolean canTransitionTo(TokenState target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case WAITING -> target == CALLED || target == CANCELLED;
            case CALLED -> target == SERVING || target == NO_SHOW || target == SKIPPED || target == CANCELLED;
            case SKIPPED -> target == CALLED || target == CANCELLED;
            case SERVING -> target == COMPLETED || target == NO_SHOW || target == SKIPPED || target == CANCELLED;
            case COMPLETED, NO_SHOW, CANCELLED -> false;
        };
    }
}
