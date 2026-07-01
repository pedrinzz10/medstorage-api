package com.saas.MedStorage_api.auth.service;

import com.saas.MedStorage_api.auth.service.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimiterTest {

    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter(5, 60);
    }

    @Test
    void firstFiveAttemptsAreAllowed() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.isAllowed("127.0.0.1"),
                    "Attempt " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void sixthAttemptIsBlocked() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.isAllowed("127.0.0.1");
        }
        assertFalse(rateLimiter.isAllowed("127.0.0.1"), "6th attempt must be blocked");
    }

    @Test
    void differentIpsAreTrackedIndependently() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.isAllowed("10.0.0.1");
        }
        assertTrue(rateLimiter.isAllowed("10.0.0.2"), "Different IP must not be rate-limited");
        assertFalse(rateLimiter.isAllowed("10.0.0.1"), "Exhausted IP must be blocked");
    }

    @Test
    void windowResetAfterExpiry() throws InterruptedException {
        // Janela de 1s: as 3 chamadas rápidas caem na mesma janela (bloqueio
        // determinístico), e o sleep > 1s garante a expiração. Usar janela 0
        // tornava o teste dependente de o relógio avançar 1ms entre chamadas.
        LoginRateLimiter shortWindow = new LoginRateLimiter(2, 1);
        shortWindow.isAllowed("192.168.1.1");
        shortWindow.isAllowed("192.168.1.1");
        assertFalse(shortWindow.isAllowed("192.168.1.1"), "Should be blocked at 3rd attempt");

        Thread.sleep(1_100);
        assertTrue(shortWindow.isAllowed("192.168.1.1"), "Window expired — should be allowed again");
    }

    @Test
    void evictExpiredCleansOldEntries() {
        LoginRateLimiter shortWindow = new LoginRateLimiter(5, 0);
        shortWindow.isAllowed("192.168.2.1");
        shortWindow.evictExpired();
        assertTrue(shortWindow.isAllowed("192.168.2.1"), "After evict, fresh window must start");
    }

    @Test
    void singleAttemptIsAlwaysAllowed() {
        assertTrue(rateLimiter.isAllowed("172.16.0.1"));
    }
}
