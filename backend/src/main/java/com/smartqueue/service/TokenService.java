package com.smartqueue.service;

import com.smartqueue.dto.IssueTokenRequest;
import com.smartqueue.dto.TokenResponse;
import com.smartqueue.model.*;
import com.smartqueue.repository.*;
import com.smartqueue.service.sms.SmsGatewayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final TokenRepository tokenRepository;
    private final SlotCapacityRepository slotCapacityRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final CounterRepository counterRepository;
    private final TokenStateTransitionRepository transitionRepository;
    private final ServiceTimeLogRepository serviceTimeLogRepository;
    private final LoadBalancerService loadBalancerService;
    private final PredictionClient predictionClient;
    private final SseEmitterService sseEmitterService;
    private final SmsGatewayProvider smsGatewayProvider;

    @Value("${queue.called-timeout-minutes:3}")
    private int calledTimeoutMinutes;

    public TokenService(TokenRepository tokenRepository,
                        SlotCapacityRepository slotCapacityRepository,
                        ServiceTypeRepository serviceTypeRepository,
                        CounterRepository counterRepository,
                        TokenStateTransitionRepository transitionRepository,
                        ServiceTimeLogRepository serviceTimeLogRepository,
                        LoadBalancerService loadBalancerService,
                        PredictionClient predictionClient,
                        SseEmitterService sseEmitterService,
                        SmsGatewayProvider smsGatewayProvider) {
        this.tokenRepository = tokenRepository;
        this.slotCapacityRepository = slotCapacityRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.counterRepository = counterRepository;
        this.transitionRepository = transitionRepository;
        this.serviceTimeLogRepository = serviceTimeLogRepository;
        this.loadBalancerService = loadBalancerService;
        this.predictionClient = predictionClient;
        this.sseEmitterService = sseEmitterService;
        this.smsGatewayProvider = smsGatewayProvider;
    }

    @Transactional
    public TokenResponse issueToken(IssueTokenRequest request) {
        UUID officeId = request.officeId();
        UUID serviceTypeId = request.serviceTypeId();
        String normalizedNic = request.citizenNic().trim().toUpperCase();
        String citizenHash = hashNic(normalizedNic);

        // 1. Idempotency Check (FR-1.4)
        String idempotencyKey = request.idempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Token> existingToken = tokenRepository.findByOfficeIdAndIdempotencyKey(officeId, idempotencyKey);
            if (existingToken.isPresent()) {
                log.info("Idempotent hit for key: {}. Returning existing token.", idempotencyKey);
                return toResponse(existingToken.get());
            }
        }

        // 2. Active Token per Citizen check (FR-1.3)
        long activeCount = tokenRepository.countActiveTokensForCitizen(officeId, citizenHash, serviceTypeId);
        if (activeCount > 0) {
            throw new IllegalStateException("Citizen already holds an active token for this service type.");
        }

        // 3. Locate or create today's slot capacity
        LocalDate today = LocalDate.now();
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Service type not found: " + serviceTypeId));

        SlotCapacity slot = slotCapacityRepository.findByOfficeIdAndServiceTypeIdAndSessionDate(officeId, serviceTypeId, today)
                .orElseGet(() -> createDefaultSlotForToday(officeId, serviceType, today));

        // 4. Atomic Capacity Reservation (FR-1.2, NFR-6.2)
        int updatedRows = slotCapacityRepository.reserveSlotAtomically(officeId, slot.getId());
        if (updatedRows == 0) {
            throw new IllegalStateException("Session capacity reached. Overbooking limit has been met for this time window.");
        }

        // 5. Generate token number (e.g. N-001, G-014)
        String prefix = serviceType.getCode().substring(0, Math.min(2, serviceType.getCode().length())).toUpperCase();
        String tokenNumber = String.format("%s-%03d", prefix, slot.getIssuedCount() + 1);

        // 6. Provisional Counter Routing (FR-2.1)
        var routingOpt = loadBalancerService.findOptimalCounter(officeId, serviceTypeId, serviceType.getDefaultDurationMinutes() * 60);
        UUID assignedCounterId = routingOpt.map(r -> r.counter().getId()).orElse(null);

        // 7. Wait Time Prediction (FR-8.1)
        long queueAhead = tokenRepository.countTokensAhead(officeId, serviceTypeId, 0, OffsetDateTime.now());
        int predictedWaitSeconds = predictionClient.predictWaitTimeSeconds(
                officeId, serviceTypeId, (int) queueAhead, serviceType.getDefaultDurationMinutes() * 60);
        OffsetDateTime estimatedCallTime = OffsetDateTime.now().plusSeconds(predictedWaitSeconds);

        // 8. Mask citizen phone for privacy (PII protection)
        String maskedPhone = maskPhone(request.citizenPhone());

        // 9. Save Token
        Token token = new Token(
                UUID.randomUUID(),
                officeId,
                tokenNumber,
                serviceTypeId,
                slot.getId(),
                citizenHash,
                maskedPhone,
                request.channel() != null ? request.channel().toUpperCase() : "WEB",
                idempotencyKey
        );
        token.setAssignedCounterId(assignedCounterId);
        token.setEstimatedCallTime(estimatedCallTime);
        token = tokenRepository.save(token);

        // 10. Audit initial state
        TokenStateTransition transition = new TokenStateTransition(
                officeId, token.getId(), "NONE", "WAITING", "CITIZEN_ISSUANCE", null, "Initial token issuance");
        transitionRepository.save(transition);

        // 11. Send outbound SMS notification if phone provided
        if (request.citizenPhone() != null && !request.citizenPhone().isBlank()) {
            String msg = String.format("SmartQueue: Your token is %s. Estimated wait: %d mins. Track online: /track/%s",
                    tokenNumber, Math.max(1, predictedWaitSeconds / 60), token.getId());
            smsGatewayProvider.sendSms(request.citizenPhone(), msg);
        }

        return toResponse(token);
    }

    @Transactional
    public TokenResponse transitionTokenState(UUID tokenId, TokenState targetState, String triggeredBy,
                                              UUID operatorId, String reason) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found: " + tokenId));

        TokenState currentState = token.getState();
        if (!currentState.canTransitionTo(targetState)) {
            throw new IllegalStateException(String.format("Invalid state transition from %s to %s", currentState, targetState));
        }

        OffsetDateTime now = OffsetDateTime.now();
        token.setState(targetState);

        switch (targetState) {
            case CALLED -> {
                token.setCalledAt(now);
                // Dispatch lead SMS alert when citizen is called
                notifyCitizenTurnApproaching(token);
            }
            case SERVING -> token.setServedAt(now);
            case COMPLETED -> {
                token.setCompletedAt(now);
                recordServiceTime(token, operatorId);
                slotCapacityRepository.decrementActiveWaitingCount(token.getSlotId());
            }
            case CANCELLED -> {
                token.setCancelledAt(now);
                slotCapacityRepository.decrementActiveWaitingCount(token.getSlotId());
            }
            case NO_SHOW -> slotCapacityRepository.decrementActiveWaitingCount(token.getSlotId());
            default -> {}
        }

        token = tokenRepository.save(token);

        TokenStateTransition transition = new TokenStateTransition(
                token.getOfficeId(), token.getId(), currentState.name(), targetState.name(),
                triggeredBy, operatorId, reason);
        transition = transitionRepository.save(transition);

        // Broadcast to SSE clients
        sseEmitterService.broadcastTokenUpdate(tokenId, "state_change", toResponse(token), transition.getId());

        return toResponse(token);
    }

    @Transactional
    public TokenResponse cancelToken(UUID tokenId, String reason) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found: " + tokenId));
        if (token.getState() != TokenState.WAITING && token.getState() != TokenState.CALLED) {
            throw new IllegalStateException("Only tokens in WAITING or CALLED state can be cancelled.");
        }
        return transitionTokenState(tokenId, TokenState.CANCELLED, "CITIZEN_OR_ADMIN", null, reason);
    }

    @Transactional
    public Optional<TokenResponse> callNextForCounter(UUID counterId, UUID operatorId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterId));

        List<Token> candidates = tokenRepository.findByOfficeIdAndState(counter.getOfficeId(), TokenState.WAITING);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Pick highest priority / oldest token
        Token nextToken = candidates.get(0);
        nextToken.setAssignedCounterId(counterId);
        tokenRepository.save(nextToken);

        TokenResponse response = transitionTokenState(
                nextToken.getId(), TokenState.CALLED, "OPERATOR", operatorId, "Called next token by operator");

        counter.setCurrentTokenId(nextToken.getId());
        counter.setCurrentOperatorId(operatorId);
        counter.setStatus(CounterStatus.BUSY);
        counterRepository.save(counter);

        return Optional.of(response);
    }

    /**
     * FR-3.5: Automated sweep transitioning CALLED tokens that timed out into NO_SHOW.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void sweepCalledTimeouts() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(calledTimeoutMinutes);
        List<Token> expired = tokenRepository.findExpiredCalledTokens(threshold);
        for (Token t : expired) {
            log.info("Token {} timed out in CALLED state. Auto-transitioning to NO_SHOW.", t.getTokenNumber());
            transitionTokenState(t.getId(), TokenState.NO_SHOW, "SYSTEM_TIMEOUT_WORKER", null, "Called response timeout exceeded");
        }
    }

    public TokenResponse getStatus(UUID tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found: " + tokenId));
        return toResponse(token);
    }

    private void notifyCitizenTurnApproaching(Token token) {
        String counterName = "Counter";
        if (token.getAssignedCounterId() != null) {
            counterName = counterRepository.findById(token.getAssignedCounterId())
                    .map(Counter::getName).orElse("Assigned Counter");
        }
        String msg = String.format("SmartQueue ALERT: Token %s is now called! Please proceed to %s immediately.",
                token.getTokenNumber(), counterName);
        // Outbound SMS trigger
        smsGatewayProvider.sendSms(token.getCitizenPhoneMasked(), msg);
    }

    private void recordServiceTime(Token token, UUID operatorId) {
        if (token.getServedAt() != null && token.getCompletedAt() != null && token.getAssignedCounterId() != null) {
            int duration = (int) Duration.between(token.getServedAt(), token.getCompletedAt()).toSeconds();
            ServiceTimeLog logRecord = new ServiceTimeLog(
                    UUID.randomUUID(),
                    token.getOfficeId(),
                    token.getAssignedCounterId(),
                    token.getServiceTypeId(),
                    token.getId(),
                    operatorId,
                    Math.max(1, duration),
                    token.getServedAt(),
                    token.getCompletedAt()
            );
            serviceTimeLogRepository.save(logRecord);
        }
    }

    private SlotCapacity createDefaultSlotForToday(UUID officeId, ServiceType serviceType, LocalDate today) {
        SlotCapacity slot = new SlotCapacity();
        slot.setId(UUID.randomUUID());
        slot.setOfficeId(officeId);
        slot.setServiceTypeId(serviceType.getId());
        slot.setSessionDate(today);
        slot.setStartTime(LocalTime.of(8, 30));
        slot.setEndTime(LocalTime.of(16, 30));
        slot.setRawCapacity(50);
        slot.setComputedLimit(58); // 15% default overbooking
        slot.setIssuedCount(0);
        slot.setActiveWaitingCount(0);
        return slotCapacityRepository.save(slot);
    }

    private TokenResponse toResponse(Token token) {
        ServiceType st = serviceTypeRepository.findById(token.getServiceTypeId()).orElse(null);
        String serviceName = st != null ? st.getName() : "Service";

        String counterNumber = null;
        if (token.getAssignedCounterId() != null) {
            counterNumber = counterRepository.findById(token.getAssignedCounterId())
                    .map(Counter::getCounterNumber).orElse(null);
        }

        int position = 0;
        if (token.getState() == TokenState.WAITING) {
            position = (int) tokenRepository.countTokensAhead(
                    token.getOfficeId(), token.getServiceTypeId(), token.getPriority(), token.getCreatedAt()) + 1;
        }

        int defaultDuration = st != null ? st.getDefaultDurationMinutes() : 15;
        int waitMins = Math.max(1, (position * defaultDuration));

        // In WAITING state, counter assignment is provisional
        boolean isProvisional = token.getState() == TokenState.WAITING;

        return new TokenResponse(
                token.getId(),
                token.getOfficeId(),
                token.getTokenNumber(),
                token.getServiceTypeId(),
                serviceName,
                token.getState(),
                token.getAssignedCounterId(),
                counterNumber,
                isProvisional,
                position,
                waitMins,
                token.getEstimatedCallTime(),
                token.getCreatedAt(),
                token.getChannel()
        );
    }

    private String hashNic(String nic) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(nic.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(nic.hashCode());
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, Math.min(3, phone.length())) + "****" + phone.substring(phone.length() - 2);
    }
}
