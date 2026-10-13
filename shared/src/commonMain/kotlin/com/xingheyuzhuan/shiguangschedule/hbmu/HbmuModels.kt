package com.xingheyuzhuan.shiguangschedule.hbmu

import kotlinx.serialization.Serializable

@Serializable
data class HbmuSavedAccount(
    val account: String = "",
    val password: String = "",
    val remember: Boolean = true,
    val autoSync: Boolean = true,
    val semester: String = "",
    val lastSync: Long = 0,
    val recordCount: Int = 0
)

data class HbmuTerm(val code: String, val label: String, val selected: Boolean = false)

object HbmuTermParser {
    fun parse(html: String): List<HbmuTerm> {
        val select = Regex("<select[^>]*(?:name|id)=[\"']xnxqdm[\"'][^>]*>(.*?)</select>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(html)?.groupValues?.get(1) ?: html
        return Regex("<option([^>]*value=[\"']([0-9]{6})[\"'][^>]*)>(.*?)</option>", RegexOption.DOT_MATCHES_ALL)
            .findAll(select).map {
                HbmuTerm(it.groupValues[2], it.groupValues[3].replace(Regex("<[^>]*>"), "").trim(),
                    Regex("\\bselected(?:\\s|=|$)", RegexOption.IGNORE_CASE).containsMatchIn(it.groupValues[1]))
            }.distinctBy { it.code }.sortedByDescending { it.selected }.toList()
    }
}
data class HbmuCaptcha(val bytes: ByteArray, val recognized: String)
data class HbmuState(
    val account: String = "",
    val semester: String = "",
    val terms: List<HbmuTerm> = emptyList(),
    val remember: Boolean = true,
    val autoSync: Boolean = true,
    val connected: Boolean = false,
    val busy: Boolean = false,
    val message: String = "登录湖北医药学院教务系统，课表随时可看",
    val error: String? = null,
    val captcha: HbmuCaptcha? = null,
    val lastSync: Long = 0,
    val recordCount: Int = 0
)

class HbmuSessionExpired : Exception("教务会话已失效，请重新登录")
class HbmuLoginException(message: String, val captchaError: Boolean = false) : Exception(message)

interface HbmuGateway {
    fun loadAccount(): HbmuSavedAccount
    fun saveAccount(account: HbmuSavedAccount)
    fun clearSession()
    suspend fun captcha(): HbmuCaptcha
    suspend fun login(account: String, password: String, captcha: String)
    suspend fun terms(): List<HbmuTerm>
    suspend fun courses(semester: String, rows: Int): String
}

expect fun createHbmuGateway(): HbmuGateway
