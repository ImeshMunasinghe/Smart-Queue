package com.smartqueue.controller;

import com.smartqueue.dto.TokenResponse;
import com.smartqueue.model.Token;
import com.smartqueue.repository.TokenRepository;
import com.smartqueue.service.TokenService;
import com.smartqueue.service.sms.SmsGatewayProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/webhooks/sms")
@CrossOrigin(origins = "*")
public class SmsWebhookController {

    private final TokenService tokenService;
    private final TokenRepository tokenRepository;
    private final SmsGatewayProvider smsGatewayProvider;

    public SmsWebhookController(TokenService tokenService,
                                TokenRepository tokenRepository,
                                SmsGatewayProvider smsGatewayProvider) {
        this.tokenService = tokenService;
        this.tokenRepository = tokenRepository;
        this.smsGatewayProvider = smsGatewayProvider;
    }

    public record InboundSmsPayload(String sender, String text) {}

    @PostMapping("/inbound")
    public ResponseEntity<Map<String, String>> handleInboundSms(@RequestBody InboundSmsPayload payload) {
        if (payload.text() == null || payload.sender() == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "IGNORED", "reason", "Missing text or sender"));
        }

        String raw = payload.text().trim().toUpperCase();
        String reply;

        if (raw.startsWith("STATUS")) {
            String[] parts = raw.split("\\s+");
            if (parts.length < 2) {
                reply = "SmartQueue: Please supply token number. Example: STATUS N-001";
            } else {
                String tokenNum = parts[1];
                Optional<Token> tokenOpt = tokenRepository.findAll().stream()
                        .filter(t -> t.getTokenNumber().equalsIgnoreCase(tokenNum))
                        .findFirst();

                if (tokenOpt.isPresent()) {
                    TokenResponse resp = tokenService.getStatus(tokenOpt.get().getId());
                    reply = String.format("SmartQueue: Token %s is in state %s. Queue position: %d. Approx wait: %d mins.",
                            resp.tokenNumber(), resp.state(), resp.queuePosition(), resp.estimatedWaitMinutes());
                } else {
                    reply = "SmartQueue: Token " + tokenNum + " not found.";
                }
            }
        } else if (raw.startsWith("CANCEL")) {
            String[] parts = raw.split("\\s+");
            if (parts.length < 2) {
                reply = "SmartQueue: Please supply token number to cancel. Example: CANCEL N-001";
            } else {
                String tokenNum = parts[1];
                Optional<Token> tokenOpt = tokenRepository.findAll().stream()
                        .filter(t -> t.getTokenNumber().equalsIgnoreCase(tokenNum))
                        .findFirst();

                if (tokenOpt.isPresent()) {
                    try {
                        tokenService.cancelToken(tokenOpt.get().getId(), "SMS Command Cancellation");
                        reply = "SmartQueue: Token " + tokenNum + " has been successfully cancelled.";
                    } catch (Exception e) {
                        reply = "SmartQueue: Unable to cancel token (" + e.getMessage() + ").";
                    }
                } else {
                    reply = "SmartQueue: Token " + tokenNum + " not found.";
                }
            }
        } else {
            reply = "SmartQueue: Commands: STATUS <TokenNumber> (e.g. STATUS N-001) or CANCEL <TokenNumber>.";
        }

        smsGatewayProvider.sendSms(payload.sender(), reply);
        return ResponseEntity.ok(Map.of("status", "PROCESSED", "reply", reply));
    }
}
