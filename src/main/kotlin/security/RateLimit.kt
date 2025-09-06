package me.m64diamondstar.security

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class RateLimitInfo(var count: Int, var lastReset: Instant)

val rateLimits = ConcurrentHashMap<String, RateLimitInfo>()

fun isAllowed(apiKey: String, limit: Int = 60): Boolean {
    val now = Instant.now()
    val info = rateLimits.computeIfAbsent(apiKey) {
        RateLimitInfo(0, now)
    }

    // Reset every minute
    if (now.isAfter(info.lastReset.plusSeconds(60))) {
        info.count = 0
        info.lastReset = now
    }

    return if (info.count < limit) {
        info.count++
        true
    } else {
        false
    }
}