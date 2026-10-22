package io.github.rainboom7.cosmetics.core.catalog

/** Sliding-window limiter: at most [maxCalls] in any [windowMillis]. Thread-safe. */
class RateLimiter(
    private val maxCalls: Int,
    private val windowMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val calls = ArrayDeque<Long>()

    fun tryAcquire(): Boolean = synchronized(calls) {
        val now = clock()
        while (calls.isNotEmpty() && now - calls.first() >= windowMillis) calls.removeFirst()
        if (calls.size >= maxCalls) return false
        calls.addLast(now)
        true
    }
}
