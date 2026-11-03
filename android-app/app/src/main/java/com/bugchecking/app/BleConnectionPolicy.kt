package com.bugchecking.app

private val reconnectDelaysMs = longArrayOf(2_000L, 8_000L)

/**
 * Returns the delay before the next connection attempt, or null once the
 * current user-started/recovery cycle has exhausted its bounded retry budget.
 * Together with the initial attempt this permits at most three attempts.
 */
internal fun reconnectDelayMs(retriesScheduled: Int): Long? =
    reconnectDelaysMs.getOrNull(retriesScheduled.coerceAtLeast(0))
