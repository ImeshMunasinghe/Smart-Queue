package com.smartqueue.dto;

import com.smartqueue.model.TokenState;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TokenResponse(
    UUID id,
    UUID officeId,
    String tokenNumber,
    UUID serviceTypeId,
    String serviceTypeName,
    TokenState state,
    UUID assignedCounterId,
    String assignedCounterNumber,
    boolean isProvisionalCounter,
    int queuePosition,
    int estimatedWaitMinutes,
    OffsetDateTime estimatedCallTime,
    OffsetDateTime createdAt,
    String channel
) {}
