package io.github.rainboom7.cosmetics.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimiterTest {
    @Test
    fun `allows a burst, then one call per freed slot`() {
        var now = 0L
        val limiter = RateLimiter(maxCalls = 3, windowMillis = 60_000) { now }
        assertEquals(listOf(true, true, true, false), List(4) { limiter.tryAcquire() })
        now = 59_999
        assertEquals(false, limiter.tryAcquire())
        now = 60_000
        assertEquals(listOf(true, true, true, false), List(4) { limiter.tryAcquire() })
    }
}
