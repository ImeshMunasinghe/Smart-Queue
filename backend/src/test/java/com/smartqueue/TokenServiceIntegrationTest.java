package com.smartqueue;

import com.smartqueue.dto.IssueTokenRequest;
import com.smartqueue.dto.TokenResponse;
import com.smartqueue.model.SlotCapacity;
import com.smartqueue.model.TokenState;
import com.smartqueue.repository.SlotCapacityRepository;
import com.smartqueue.repository.TokenRepository;
import com.smartqueue.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(MockRedisConfig.class)
public class TokenServiceIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private SlotCapacityRepository slotCapacityRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final UUID officeId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private final UUID nicServiceId = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    @BeforeEach
    void cleanTokens() {
        tokenRepository.deleteAll();
    }

    @Test
    void testIssueToken_DualNicFormat_Success() {
        // 1. Legacy 9-digit + V format
        IssueTokenRequest req1 = new IssueTokenRequest(
                officeId, nicServiceId, "145896235V", "0771234567", "WEB", "idem-legacy"
        );
        TokenResponse res1 = tokenService.issueToken(req1);
        assertNotNull(res1);
        assertNotNull(res1.id());
        assertEquals(TokenState.WAITING, res1.state());

        // 2. Modern 12-digit format
        IssueTokenRequest req2 = new IssueTokenRequest(
                officeId, nicServiceId, "144756235896", "0777654321", "WEB", "idem-modern"
        );
        TokenResponse res2 = tokenService.issueToken(req2);
        assertNotNull(res2);
        assertNotNull(res2.id());
        assertEquals(TokenState.WAITING, res2.state());
    }

    @Test
    void testIdempotency_SameKeyReturnsIdenticalToken() {
        String key = "unique-client-key-999";
        IssueTokenRequest req = new IssueTokenRequest(
                officeId, nicServiceId, "123456789V", "0711111111", "WEB", key
        );

        TokenResponse first = tokenService.issueToken(req);
        TokenResponse second = tokenService.issueToken(req);

        assertEquals(first.id(), second.id());
        assertEquals(first.tokenNumber(), second.tokenNumber());
    }

    @Test
    void testActiveTokenConstraint_RejectsDuplicateActiveToken() {
        IssueTokenRequest req1 = new IssueTokenRequest(
                officeId, nicServiceId, "888888888V", "0722222222", "WEB", null
        );
        tokenService.issueToken(req1);

        IssueTokenRequest req2 = new IssueTokenRequest(
                officeId, nicServiceId, "888888888V", "0722222222", "WEB", null
        );

        assertThrows(IllegalStateException.class, () -> tokenService.issueToken(req2));
    }

    @Test
    void testTokenLifecycle_ValidTransitionsAndCancellation() {
        IssueTokenRequest req = new IssueTokenRequest(
                officeId, nicServiceId, "777777777V", "0733333333", "WEB", null
        );
        TokenResponse issued = tokenService.issueToken(req);
        UUID tokenId = issued.id();

        // WAITING -> CALLED
        TokenResponse called = tokenService.transitionTokenState(
                tokenId, TokenState.CALLED, "OPERATOR", UUID.randomUUID(), "Calling citizen");
        assertEquals(TokenState.CALLED, called.state());

        // CALLED -> SERVING
        TokenResponse serving = tokenService.transitionTokenState(
                tokenId, TokenState.SERVING, "OPERATOR", UUID.randomUUID(), "Serving citizen");
        assertEquals(TokenState.SERVING, serving.state());

        // SERVING -> COMPLETED
        TokenResponse completed = tokenService.transitionTokenState(
                tokenId, TokenState.COMPLETED, "OPERATOR", UUID.randomUUID(), "Consultation finished");
        assertEquals(TokenState.COMPLETED, completed.state());

        // Illegal transition from COMPLETED -> WAITING must fail
        assertThrows(IllegalStateException.class, () -> {
            tokenService.transitionTokenState(tokenId, TokenState.WAITING, "OPERATOR", null, "Illegal retry");
        });
    }

    @Test
    void testCitizenCancellation_ReclaimsWaitingCapacity() {
        IssueTokenRequest req = new IssueTokenRequest(
                officeId, nicServiceId, "555555555V", "0755555555", "WEB", null
        );
        TokenResponse token = tokenService.issueToken(req);

        TokenResponse cancelled = tokenService.cancelToken(token.id(), "Citizen withdrew");
        assertEquals(TokenState.CANCELLED, cancelled.state());
    }

    @Test
    void testConcurrentIssuance_ZeroOverselling() throws InterruptedException {
        // Create an isolated slot with a strict capacity of exactly 5 slots
        SlotCapacity smallSlot = new SlotCapacity();
        smallSlot.setId(UUID.randomUUID());
        smallSlot.setOfficeId(officeId);
        smallSlot.setServiceTypeId(nicServiceId);
        smallSlot.setSessionDate(LocalDate.now().plusDays(10)); // Distinct future date
        smallSlot.setStartTime(java.time.LocalTime.of(9, 0));
        smallSlot.setEndTime(java.time.LocalTime.of(10, 0));
        smallSlot.setRawCapacity(5);
        smallSlot.setComputedLimit(5);
        smallSlot.setManualOverrideLimit(5);
        smallSlot.setIssuedCount(0);
        smallSlot.setActiveWaitingCount(0);
        smallSlot = slotCapacityRepository.save(smallSlot);

        final UUID testSlotId = smallSlot.getId();
        int totalRequests = 25;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to fire simultaneously
                    Integer updated = txTemplate.execute(status ->
                            slotCapacityRepository.reserveSlotAtomically(officeId, testSlotId));
                    if (updated != null && updated > 0) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Fire all threads concurrently
        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 5 reservations must succeed, 20 must be rejected
        assertEquals(5, successCount.get(), "Must not oversell beyond capacity limit");
        assertEquals(20, rejectedCount.get(), "Must reject all requests exceeding capacity");

        // Verify database state
        SlotCapacity verified = slotCapacityRepository.findById(testSlotId).orElseThrow();
        assertEquals(5, verified.getIssuedCount());
    }
}
