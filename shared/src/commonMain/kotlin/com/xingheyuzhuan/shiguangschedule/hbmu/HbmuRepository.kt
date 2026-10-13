// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.hbmu

import androidx.room3.withWriteTransaction
import com.xingheyuzhuan.shiguangschedule.data.db.main.*
import com.xingheyuzhuan.shiguangschedule.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single
class HbmuRepository(
    private val database: MainAppDatabase,
    private val conversion: CourseConversionRepository,
    private val settings: AppSettingsRepository,
    private val timeSchedules: TimeScheduleRepository
) {
    private val gateway by lazy { createHbmuGateway() }
    private val mutex = Mutex()
    private var saved = gateway.loadAccount()
    private var termChosen = saved.semester.isNotBlank()
    private var activePassword = saved.password
    private val state = MutableStateFlow(HbmuState(
        account = saved.account, semester = saved.semester.ifBlank { defaultSemester() },
        remember = saved.remember, autoSync = saved.autoSync, lastSync = saved.lastSync, recordCount = saved.recordCount,
        displayName = saved.displayName, signedIn = saved.signedIn || (saved.account.isNotBlank() && saved.lastSync > 0)
    ))
    val uiState = state.asStateFlow()

    private fun defaultSemester(): String {
        val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return if (date.month.ordinal + 1 >= 8) "${date.year}01" else "${date.year - 1}02"
    }

    private suspend fun operation(action: suspend () -> Unit) = mutex.withLock { performAction(action) }

    private suspend fun performAction(action: suspend () -> Unit) {
        state.update { it.copy(busy = true, error = null) }
        try { action() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            state.update { it.copy(error = e.message ?: "连接失败，请稍后再试") }
        } finally { state.update { it.copy(busy = false) } }
    }

    suspend fun refreshCaptcha() = operation {
        state.update { it.copy(message = "正在获取并识别验证码…") }
        val captcha = gateway.captcha()
        state.update { it.copy(captcha = captcha, message = if (captcha.recognized.length == 4) "验证码已在本机识别，可直接登录" else "可手动填写验证码后登录") }
    }

    suspend fun signIn(account: String, password: String, code: String, remember: Boolean, autoSync: Boolean) = operation {
        require(account.isNotBlank() && password.isNotBlank()) { "请填写学号和教务密码" }
        activePassword = password
        state.update { it.copy(account = account.trim(), remember = remember, autoSync = autoSync, connected = false, signedIn = false, displayName = "", message = "正在登录教务系统…") }
        if (code.isBlank()) automaticLogin(account.trim(), password)
        else gateway.login(account.trim(), password, code.trim())
        state.update { it.copy(connected = true, signedIn = true, captcha = null) }
        persist()
        loadTerms()
        syncInternal()
    }

    private suspend fun automaticLogin(account: String, password: String) {
        repeat(3) { attempt ->
            state.update { it.copy(message = "自动识别验证码并登录（${attempt + 1}/3）…") }
            val captcha = gateway.captcha()
            state.update { it.copy(captcha = captcha) }
            if (!captcha.recognized.matches(Regex("[A-Za-z0-9]{4}"))) return@repeat
            try {
                gateway.login(account, password, captcha.recognized)
                state.update { it.copy(connected = true, captcha = null) }
                return
            } catch (e: HbmuLoginException) {
                if (!e.captchaError) throw e
            }
        }
        throw HbmuLoginException("自动识别未通过，请手动填写验证码再登录", true)
    }

    private suspend fun loadTerms() {
        val terms = gateway.terms()
        state.update { current -> current.copy(terms = terms, displayName = gateway.displayName().ifBlank { current.displayName }, semester = when {
            terms.isEmpty() -> current.semester
            !termChosen -> terms.firstOrNull { it.selected }?.code ?: current.semester
            terms.none { it.code == current.semester } -> terms.first().code
            else -> current.semester
        }) }
        if (terms.isNotEmpty()) termChosen = true
        persist()
    }

    suspend fun selectSemester(code: String) {
        mutex.withLock {
            require(code.matches(Regex("[0-9]{6}")))
            termChosen = true
            state.update { it.copy(semester = code) }
            persist()
        }
    }

    suspend fun sync() = operation { syncInternal() }

    private suspend fun syncInternal() {
        state.update { it.copy(message = "正在核对并同步整学期课表…") }
        if (state.value.terms.isEmpty()) {
            try { loadTerms() } catch (e: HbmuSessionExpired) {
                state.update { it.copy(connected = false) }
                require(state.value.remember && activePassword.isNotBlank()) { "登录已过期，请在账号页重新登录" }
                automaticLogin(state.value.account, activePassword)
                loadTerms()
            }
        }
        val semester = state.value.semester
        require(semester.matches(Regex("[0-9]{6}"))) { "请选择有效学期" }
        var body = try { gateway.courses(semester, 1000) } catch (e: HbmuSessionExpired) {
            state.update { it.copy(connected = false) }
            require(state.value.remember && activePassword.isNotBlank()) { "登录已过期，请在教务页重新登录" }
            automaticLogin(state.value.account, activePassword)
            gateway.courses(semester, 1000)
        }
        // Request the whole list again if the server reports a larger total; never trust unstable pagination.
        val total = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(body)
            .let { it as kotlinx.serialization.json.JsonObject }["total"]
            .let { (it as kotlinx.serialization.json.JsonPrimitive).content.toInt() } }.getOrDefault(-1)
        if (total in 1001..10000) body = gateway.courses(semester, total)
        val parsed = HbmuScheduleParser.parse(body)
        val tableId = "hbmu-${state.value.account}-$semester"
        database.withWriteTransaction {
            if (database.courseTableDao().getCourseTableById(tableId) == null) {
                database.courseTableDao().insert(CourseTable(tableId, "HBMU · $semester", Clock.System.now().toEpochMilliseconds()))
                timeSchedules.saveExclusiveTimeTable(
                    TimeTable(id = tableId, name = null, createdAt = Clock.System.now().toEpochMilliseconds()),
                    defaultTimeSlots.map { it.copy(timeTableId = tableId) }
                )
            }
            conversion.importCourseTableFromJson(tableId, parsed.model)
            val config = settings.getCourseConfigOnce(tableId) ?: CourseTableConfig(tableId)
            settings.insertOrUpdateCourseConfig(config.copy(showWeekends = parsed.hasWeekends))
        }
        val app = settings.getAppSettingsOnce()
        settings.insertOrUpdateAppSettings(app.copy(currentCourseTableId = tableId))
        state.update { it.copy(connected = true, signedIn = true, lastSync = Clock.System.now().toEpochMilliseconds(),
            recordCount = parsed.total, message = "已同步 ${parsed.total} 次排课，可离线查看", captcha = null) }
        persist()
    }

    suspend fun resume() = mutex.withLock {
        if (!state.value.autoSync || !state.value.remember || activePassword.isBlank()) return@withLock
        if (state.value.connected && Clock.System.now().toEpochMilliseconds() - state.value.lastSync < 30 * 60 * 1000) return@withLock
        performAction { syncInternal() }
    }

    suspend fun updatePreferences(remember: Boolean, autoSync: Boolean) = mutex.withLock {
        state.update { it.copy(remember = remember, autoSync = autoSync) }
        persist()
    }

    suspend fun signOut() = mutex.withLock {
        gateway.clearSession()
        activePassword = ""
        saved = HbmuSavedAccount(remember = state.value.remember, autoSync = state.value.autoSync, semester = state.value.semester)
        gateway.saveAccount(saved)
        state.update { it.copy(account = "", connected = false, signedIn = false, displayName = "", captcha = null, error = null, message = "已退出登录，本地课表仍可查看") }
    }

    private fun persist() {
        val current = state.value
        saved = HbmuSavedAccount(current.account, if (current.remember) activePassword else "", current.remember,
            current.autoSync, current.semester, current.lastSync, current.recordCount, current.displayName, current.signedIn)
        gateway.saveAccount(saved)
    }
}
