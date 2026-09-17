package com.smartqueue.controller;

import com.smartqueue.dto.TokenResponse;
import com.smartqueue.model.Counter;
import com.smartqueue.model.CounterStatus;
import com.smartqueue.model.Token;
import com.smartqueue.model.TokenState;
import com.smartqueue.repository.CounterRepository;
import com.smartqueue.repository.TokenRepository;
import com.smartqueue.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operator")
@CrossOrigin(origins = "*")
public class OperatorController {

    private final TokenService tokenService;
    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;
    private final com.smartqueue.service.LoadBalancerService loadBalancerService;

    public OperatorController(TokenService tokenService,
                              CounterRepository counterRepository,
                              TokenRepository tokenRepository,
                              com.smartqueue.service.LoadBalancerService loadBalancerService) {
        this.tokenService = tokenService;
        this.counterRepository = counterRepository;
        this.tokenRepository = tokenRepository;
        this.loadBalancerService = loadBalancerService;
    }

    @GetMapping("/counters/{counterId}")
    public ResponseEntity<Counter> getCounterDetails(@PathVariable UUID counterId) {
        return counterRepository.findById(counterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/offices/{officeId}/counters")
    public ResponseEntity<List<Counter>> getOfficeCounters(@PathVariable UUID officeId) {
        return ResponseEntity.ok(counterRepository.findByOfficeId(officeId));
    }

    @PostMapping("/counters/{counterId}/call-next")
    public ResponseEntity<TokenResponse> callNext(
            @PathVariable UUID counterId,
            @RequestParam(required = false) UUID operatorId) {
        return tokenService.callNextForCounter(counterId, operatorId != null ? operatorId : UUID.randomUUID())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/tokens/{tokenId}/serve")
    public ResponseEntity<TokenResponse> startServing(
            @PathVariable UUID tokenId,
            @RequestParam(required = false) UUID operatorId) {
        TokenResponse res = tokenService.transitionTokenState(
                tokenId, TokenState.SERVING, "OPERATOR", operatorId, "Operator confirmed citizen attendance");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/tokens/{tokenId}/complete")
    public ResponseEntity<TokenResponse> completeToken(
            @PathVariable UUID tokenId,
            @RequestParam(required = false) UUID operatorId) {
        TokenResponse res = tokenService.transitionTokenState(
                tokenId, TokenState.COMPLETED, "OPERATOR", operatorId, "Service consultation completed");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/tokens/{tokenId}/skip")
    public ResponseEntity<TokenResponse> skipToken(
            @PathVariable UUID tokenId,
            @RequestParam(required = false) UUID operatorId) {
        TokenResponse res = tokenService.transitionTokenState(
                tokenId, TokenState.SKIPPED, "OPERATOR", operatorId, "Citizen temporarily absent, placed in skip pool");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/tokens/{tokenId}/recall")
    public ResponseEntity<TokenResponse> recallToken(
            @PathVariable UUID tokenId,
            @RequestParam(required = false) UUID operatorId) {
        TokenResponse res = tokenService.transitionTokenState(
                tokenId, TokenState.CALLED, "OPERATOR", operatorId, "Recalled skipped citizen");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/tokens/{tokenId}/no-show")
    public ResponseEntity<TokenResponse> markNoShow(
            @PathVariable UUID tokenId,
            @RequestParam(required = false) UUID operatorId) {
        TokenResponse res = tokenService.transitionTokenState(
                tokenId, TokenState.NO_SHOW, "OPERATOR", operatorId, "Citizen marked as no-show by operator");
        return ResponseEntity.ok(res);
    }

    @PutMapping("/counters/{counterId}/status")
    public ResponseEntity<Counter> updateCounterStatus(
            @PathVariable UUID counterId,
            @RequestBody Map<String, String> body) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterId));

        if (body.containsKey("status")) {
            counter.setStatus(CounterStatus.valueOf(body.get("status").toUpperCase()));
            counter = counterRepository.save(counter);
        }
        return ResponseEntity.ok(counter);
    }

    @GetMapping("/counters/{counterId}/queue")
    public ResponseEntity<List<Token>> getUpcomingQueue(@PathVariable UUID counterId) {
        Counter counter = counterRepository.findById(counterId).orElse(null);
        if (counter == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Token> queue = tokenRepository.findByOfficeIdAndState(counter.getOfficeId(), TokenState.WAITING)
                .stream()
                .filter(t -> counterId.equals(t.getAssignedCounterId())
                        || (t.getAssignedCounterId() == null && loadBalancerService.isEligible(counter, t.getServiceTypeId())))
                .sorted((a, b) -> {
                    boolean aAssigned = counterId.equals(a.getAssignedCounterId());
                    boolean bAssigned = counterId.equals(b.getAssignedCounterId());
                    if (aAssigned != bAssigned) return aAssigned ? -1 : 1;
                    int prio = Integer.compare(b.getPriority(), a.getPriority());
                    if (prio != 0) return prio;
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .toList();

        return ResponseEntity.ok(queue);
    }
}
