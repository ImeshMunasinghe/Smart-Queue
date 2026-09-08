package com.smartqueue.controller;

import com.smartqueue.dto.IssueTokenRequest;
import com.smartqueue.dto.TokenResponse;
import com.smartqueue.service.SseEmitterService;
import com.smartqueue.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tokens")
@CrossOrigin(origins = "*")
public class TokenController {

    private final TokenService tokenService;
    private final SseEmitterService sseEmitterService;

    public TokenController(TokenService tokenService, SseEmitterService sseEmitterService) {
        this.tokenService = tokenService;
        this.sseEmitterService = sseEmitterService;
    }

    @PostMapping
    public ResponseEntity<TokenResponse> issueToken(@Valid @RequestBody IssueTokenRequest request) {
        TokenResponse response = tokenService.issueToken(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tokenId}/status")
    public ResponseEntity<TokenResponse> getStatus(@PathVariable UUID tokenId) {
        return ResponseEntity.ok(tokenService.getStatus(tokenId));
    }

    @GetMapping(value = "/{tokenId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTokenUpdates(
            @PathVariable UUID tokenId,
            @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId) {
        return sseEmitterService.subscribe(tokenId, lastEventId);
    }

    @PostMapping("/{tokenId}/cancel")
    public ResponseEntity<TokenResponse> cancelToken(
            @PathVariable UUID tokenId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null && body.containsKey("reason") ? body.get("reason") : "Citizen self-cancellation";
        TokenResponse response = tokenService.cancelToken(tokenId, reason);
        return ResponseEntity.ok(response);
    }
}
