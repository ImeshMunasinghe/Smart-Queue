package com.smartqueue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record IssueTokenRequest(
    @NotNull(message = "Office ID is required")
    UUID officeId,

    @NotNull(message = "Service Type ID is required")
    UUID serviceTypeId,

    @NotBlank(message = "NIC number is required")
    @Pattern(
        regexp = "^([0-9]{9}[vVxX]|[0-9]{12})$",
        message = "Invalid NIC format. Must be legacy 9-digit format (e.g. 145896235V) or 12-digit format (e.g. 144756235896)"
    )
    String citizenNic,

    @NotBlank(message = "Phone number is required")
    String citizenPhone,

    String channel, // WEB, USSD, SMS

    String idempotencyKey
) {}
