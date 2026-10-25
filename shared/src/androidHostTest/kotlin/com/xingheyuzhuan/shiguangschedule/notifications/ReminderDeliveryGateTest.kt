package com.xingheyuzhuan.shiguangschedule.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ReminderDeliveryGateTest {
    private class Store : ReminderDeliveryStore {
        var entries: Map<String, Long> = emptyMap()
        var writable = true
        override fun read(): Map<String, Long> = entries
        override fun write(deliveries: Map<String, Long>): Boolean {
            if (!writable) return false
            entries = deliveries.toMap()
            return true
        }
    }

    @Test fun repeatedBroadcastsAndProcessRestartCannotReplayDeliveredLesson() = runBlocking {
        val store = Store()
        val gate = ReminderDeliveryGate(store)
        var alerts = 0
        assertTrue(gate.deliverOnce("lesson", 100, 1000) { alerts++; true })
        repeat(5) { assertFalse(gate.deliverOnce("lesson", 101, 1000) { alerts++; true }) }
        // Dismissing the notification does not remove the persisted delivery record.
        val restarted = ReminderDeliveryGate(store)
        assertFalse(restarted.deliverOnce("lesson", 102, 1000) { alerts++; true })
        assertEquals(1, alerts)
        assertTrue(restarted.wasDelivered("lesson", 102))
    }

    @Test fun concurrentReceiverCallbacksPostOnlyOneAlert() = runBlocking {
        val gate = ReminderDeliveryGate(Store())
        var alerts = 0
        val results = (1..32).map {
            async(Dispatchers.Default) { gate.deliverOnce("same-lesson", 100, 1000) { alerts++; true } }
        }.awaitAll()
        assertEquals(1, results.count { it })
        assertEquals(1, alerts)
    }

    @Test fun nextOccurrenceStillNotifies() = runBlocking {
        val gate = ReminderDeliveryGate(Store())
        assertTrue(gate.deliverOnce("monday-lesson", 100, 1000) { true })
        assertTrue(gate.deliverOnce("tuesday-lesson", 200, 2000) { true })
    }

    @Test fun missingPermissionDoesNotConsumeReminder() = runBlocking {
        val gate = ReminderDeliveryGate(Store())
        assertFalse(gate.deliverOnce("lesson", 100, 1000) { false })
        assertFalse(gate.wasDelivered("lesson", 101))
        assertTrue(gate.deliverOnce("lesson", 102, 1000) { true })
    }

    @Test fun failedNotificationCanBeRetried() = runBlocking {
        val gate = ReminderDeliveryGate(Store())
        try {
            gate.deliverOnce("lesson", 100, 1000) { throw IllegalStateException("Notification rejected") }
            fail("Posting must throw")
        } catch (_: IllegalStateException) { }
        assertTrue(gate.deliverOnce("lesson", 101, 1000) { true })
    }

    @Test fun storageFailureNeverPostsAnUntrackedAlert() = runBlocking {
        val store = Store().apply { writable = false }
        var posted = false
        assertFalse(ReminderDeliveryGate(store).deliverOnce("lesson", 100, 1000) { posted = true; true })
        assertFalse(posted)
        store.writable = true
        assertTrue(ReminderDeliveryGate(store).deliverOnce("lesson", 101, 1000) { true })
    }

    @Test fun expiredHistoryIsPrunedAndExpiredAlarmIsIgnored() = runBlocking {
        val store = Store().apply { entries = mapOf("old" to 99, "current" to 1000) }
        val gate = ReminderDeliveryGate(store)
        assertFalse(gate.deliverOnce("expired", 100, 100) { fail("Expired reminder must not post") })
        assertFalse(gate.wasDelivered("old", 100))
        assertTrue(gate.deliverOnce("new", 100, 2000) { true })
        assertEquals(mapOf("current" to 1000L, "new" to 2000L), store.entries)
    }
}
