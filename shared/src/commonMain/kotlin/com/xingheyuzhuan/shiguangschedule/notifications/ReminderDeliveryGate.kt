package com.xingheyuzhuan.shiguangschedule.notifications

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ReminderDeliveryStore {
    fun read(): Map<String, Long>
    fun write(deliveries: Map<String, Long>): Boolean
}

/** Serializes duplicate callbacks and remembers delivery across process restarts. */
class ReminderDeliveryGate(private val store: ReminderDeliveryStore) {
    private val mutex = Mutex()

    suspend fun wasDelivered(key: String, nowMillis: Long): Boolean = mutex.withLock {
        (store.read()[key] ?: Long.MIN_VALUE) > nowMillis
    }

    suspend fun deliverOnce(key: String, nowMillis: Long, expiresAtMillis: Long,
        post: () -> Boolean): Boolean = mutex.withLock {
        if (key.isEmpty() || expiresAtMillis <= nowMillis) return@withLock false
        val previous = store.read().filterValues { it > nowMillis }
        if (key in previous) return@withLock false
        // Persist before notifying: even a process restart must not replay the same alert.
        // Android cannot atomically commit a preference and post a system notification.
        if (!store.write(previous + (key to expiresAtMillis))) return@withLock false
        var posted = false
        try {
            posted = post()
            posted
        } finally {
            if (!posted) store.write(previous)
        }
    }
}
