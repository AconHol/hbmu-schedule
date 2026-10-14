package com.xingheyuzhuan.shiguangschedule.hbmu

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class HbmuLoginFlowTest {
    private class FakeGateway(val failure: (Int) -> Exception? = { null }, val codes: List<String> = listOf("abcd")) : HbmuGateway {
        var images = 0
        val submitted = mutableListOf<String>()
        override fun loadAccount() = HbmuSavedAccount()
        override fun saveAccount(account: HbmuSavedAccount) {}
        override fun clearSession() {}
        override suspend fun captcha() = HbmuCaptcha(byteArrayOf((++images).toByte()), codes[(images - 1).coerceAtMost(codes.lastIndex)])
        override suspend fun login(account: String, password: String, captcha: String) {
            submitted += captcha
            failure(submitted.size)?.let { throw it }
        }
        override suspend fun terms() = emptyList<HbmuTerm>()
        override suspend fun courses(semester: String, rows: Int) = ""
        override suspend fun calendar(startDate: String, endDateExclusive: String) = "[]"
    }
    @Test fun firstSuccessDoesNotFetchAnotherImage() = runBlocking {
        val gateway = FakeGateway()
        HbmuLoginFlow(gateway).automatic("demo", "example", {}, { _, _ -> })
        assertEquals(1, gateway.images)
        assertEquals(listOf("abcd"), gateway.submitted)
    }
    @Test fun captchaFailureFetchesADifferentImageAndRetries() = runBlocking {
        val gateway = FakeGateway({ if (it == 1) HbmuLoginException("验证码错误", true) else null }, listOf("abcd", "ef12"))
        val attempts = mutableListOf<Int>()
        HbmuLoginFlow(gateway).automatic("demo", "example", { attempts += it }, { _, _ -> })
        assertEquals(listOf(1, 2), attempts)
        assertEquals(listOf("abcd", "ef12"), gateway.submitted)
        assertEquals(2, gateway.images)
    }
    @Test fun threeFailuresShowAFreshUnfilledManualImage() = runBlocking {
        val gateway = FakeGateway({ HbmuLoginException("验证码错误", true) })
        var manual: HbmuCaptcha? = null
        assertFailsWith<HbmuLoginException> {
            HbmuLoginFlow(gateway).automatic("demo", "example", {}, { captcha, required -> if (required) manual = captcha })
        }
        assertEquals(3, gateway.submitted.size)
        assertEquals(4, gateway.images)
        assertEquals(4, manual!!.bytes[0].toInt())
        assertEquals("", manual!!.recognized)
    }
    @Test fun unusableOcrConsumesAttemptsWithoutSendingInvalidCodes() = runBlocking {
        val gateway = FakeGateway(codes = listOf("", "a", "ABCDE", "abcd"))
        var manual = false
        assertFailsWith<HbmuLoginException> { HbmuLoginFlow(gateway).automatic("demo", "example", {}, { _, required -> manual = required }) }
        assertTrue(gateway.submitted.isEmpty())
        assertEquals(4, gateway.images)
        assertTrue(manual)
    }
    @Test fun wrongPasswordIsNotRetried() = runBlocking {
        val gateway = FakeGateway({ HbmuLoginException("密码错误") })
        assertFailsWith<HbmuLoginException> { HbmuLoginFlow(gateway).automatic("demo", "example", {}, { _, _ -> }) }
        assertEquals(1, gateway.images)
        assertEquals(1, gateway.submitted.size)
    }
    @Test fun networkFailureIsNotRetriedAsCaptchaFailure() = runBlocking {
        val gateway = FakeGateway({ IllegalStateException("network") })
        assertFailsWith<IllegalStateException> { HbmuLoginFlow(gateway).automatic("demo", "example", {}, { _, _ -> }) }
        assertEquals(1, gateway.images)
    }
    @Test fun cancellationPropagates() = runBlocking {
        val gateway = FakeGateway({ CancellationException("cancel") })
        assertFailsWith<CancellationException> { HbmuLoginFlow(gateway).automatic("demo", "example", {}, { _, _ -> }) }
        assertEquals(1, gateway.images)
    }
}
