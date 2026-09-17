package com.smartqueue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartqueue.model.Counter;
import com.smartqueue.model.CounterStatus;
import com.smartqueue.model.Token;
import com.smartqueue.model.TokenState;
import com.smartqueue.repository.CounterRepository;
import com.smartqueue.repository.TokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class LoadBalancerService {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerService.class);
    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;
    private final ObjectMapper objectMapper;

    public LoadBalancerService(CounterRepository counterRepository,
                               TokenRepository tokenRepository,
                               ObjectMapper objectMapper) {
        this.counterRepository = counterRepository;
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
    }

    public record CounterAssignmentResult(Counter counter, int estimatedWaitSeconds) {}

    /**
     * FR-2.1 & FR-2.2: Finds the eligible online counter with the lowest Estimated Completion Time (ECT).
     */
    public Optional<CounterAssignmentResult> findOptimalCounter(UUID officeId, UUID serviceTypeId, int defaultDurationSeconds) {
        List<Counter> activeCounters = counterRepository.findByOfficeIdAndStatusIn(
                officeId, List.of(CounterStatus.ONLINE, CounterStatus.BUSY));

        List<Counter> eligibleCounters = activeCounters.stream()
                .filter(counter -> isEligible(counter, serviceTypeId))
                .toList();

        if (eligibleCounters.isEmpty()) {
            return Optional.empty();
        }

        Counter bestCounter = null;
        int minEctSeconds = Integer.MAX_VALUE;

        for (Counter counter : eligibleCounters) {
            int ect = calculateEstimatedCompletionTime(counter, defaultDurationSeconds);
            if (ect < minEctSeconds) {
                minEctSeconds = ect;
                bestCounter = counter;
            }
        }

        return bestCounter != null
                ? Optional.of(new CounterAssignmentResult(bestCounter, minEctSeconds))
                : Optional.empty();
    }

    private int calculateEstimatedCompletionTime(Counter counter, int defaultDurationSeconds) {
        int totalSeconds = 0;

        // 1. Remaining time of currently serving token (if any)
        if (counter.getCurrentTokenId() != null) {
            Optional<Token> currentTokenOpt = tokenRepository.findById(counter.getCurrentTokenId());
            if (currentTokenOpt.isPresent() && currentTokenOpt.get().getState() == TokenState.SERVING) {
                Token current = currentTokenOpt.get();
                if (current.getServedAt() != null) {
                    long elapsed = Duration.between(current.getServedAt(), OffsetDateTime.now()).toSeconds();
                    long remaining = Math.max(15, defaultDurationSeconds - elapsed);
                    totalSeconds += (int) remaining;
                } else {
                    totalSeconds += defaultDurationSeconds;
                }
            }
        }

        // 2. Sum of service times for all tokens currently queued specifically for this counter
        List<Token> queued = tokenRepository.findByAssignedCounterIdAndState(counter.getId(), TokenState.WAITING);
        totalSeconds += queued.size() * defaultDurationSeconds;

        return totalSeconds;
    }

    public boolean isEligible(Counter counter, UUID serviceTypeId) {
        try {
            List<String> eligibleIds = objectMapper.readValue(
                    counter.getEligibleServiceTypesJson(),
                    new TypeReference<List<String>>() {}
            );
            if (eligibleIds == null || eligibleIds.isEmpty()) {
                return true; // Empty indicates general eligibility for all types
            }
            return eligibleIds.contains(serviceTypeId.toString());
        } catch (Exception e) {
            log.warn("Failed to parse eligible service types for counter {}: {}", counter.getId(), e.getMessage());
            return true;
        }
    }
}
