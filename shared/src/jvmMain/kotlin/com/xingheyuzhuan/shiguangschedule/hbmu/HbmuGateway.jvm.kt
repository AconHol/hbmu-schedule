// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.hbmu

actual fun createHbmuGateway(): HbmuGateway = UnsupportedHbmuGateway()

private class UnsupportedHbmuGateway : HbmuGateway {
    override fun loadAccount() = HbmuSavedAccount()
    override fun saveAccount(account: HbmuSavedAccount) {}
    override fun clearSession() {}
    override suspend fun captcha(): HbmuCaptcha = error("请使用 Android 版连接 HBMU 教务")
    override suspend fun login(account: String, password: String, captcha: String): Unit = error("请使用 Android 版连接 HBMU 教务")
    override suspend fun terms(): List<HbmuTerm> = emptyList()
    override suspend fun courses(semester: String, rows: Int): String = error("请使用 Android 版连接 HBMU 教务")
    override suspend fun calendar(startDate: String, endDateExclusive: String): String = error("请使用 Android 版连接 HBMU 教务")
}
