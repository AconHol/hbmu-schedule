// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.hbmu

import androidx.room3.withWriteTransaction
import com.xingheyuzhuan.shiguangschedule.data.db.main.*
import com.xingheyuzhuan.shiguangschedule.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    fun rememberedPassword(): String = if (saved.remember && saved.account == state.value.account) saved.password else ""

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
        state.update { it.copy(requiresManualCaptcha = true, message = "正在获取验证码…") }
        val captcha = gateway.captcha().copy(recognized = "")
        state.update { it.copy(captcha = captcha, message = "请输入图片中的四位数字或小写字母") }
    }

    fun useAutomaticCaptcha() { state.update { it.copy(requiresManualCaptcha = false, captcha = null, error = null) } }

    suspend fun signIn(account: String, password: String, code: String, remember: Boolean, autoSync: Boolean) = operation {
        require(account.isNotBlank() && password.isNotBlank()) { "请填写学号和教务密码" }
        if (account.trim() != state.value.account) activePassword = ""
        state.update { it.copy(account = account.trim(), remember = remember, autoSync = autoSync, connected = false, signedIn = false, displayName = "", message = "正在登录教务系统…") }
        if (!state.value.requiresManualCaptcha) automaticLogin(account.trim(), password)
        else {
            require(code.matches(Regex("[0-9a-zA-Z]{4}"))) { "请填写四位验证码" }
            try { gateway.login(account.trim(), password, code.trim()) }
            catch (e: HbmuLoginException) {
                if (e.captchaError) {
                    val fresh = gateway.captcha().copy(recognized = "")
                    state.update { it.copy(captcha = fresh) }
                }
                throw e
            }
        }
        activePassword = password
        state.update { it.copy(connected = true, signedIn = true, captcha = null, requiresManualCaptcha = false) }
        persist()
        loadTerms()
        syncInternal()
    }

    private suspend fun automaticLogin(account: String, password: String) {
        state.update { it.copy(requiresManualCaptcha = false) }
        HbmuLoginFlow(gateway).automatic(account, password,
            onAttempt = { attempt -> state.update { it.copy(message = "自动识别验证码并登录（$attempt/3）…") } },
            onCaptcha = { captcha, manual -> state.update { it.copy(captcha = captcha, requiresManualCaptcha = manual) } })
        state.update { it.copy(connected = true, captcha = null, requiresManualCaptcha = false) }
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
        state.update { it.copy(message = "正在同步学校上课时间…") }
        val calendar = mutableListOf<String>()
        parsed.model.config?.semesterStartDate?.let { start ->
            val first = kotlinx.datetime.LocalDate.parse(start)
            val finalDay = first.toEpochDays() + (parsed.model.config?.semesterTotalWeeks ?: 20) * 7L
            var cursor = first.toEpochDays()
            while (cursor < finalDay) {
                val end = minOf(cursor + 28L, finalDay)
                val date1 = kotlinx.datetime.LocalDate.fromEpochDays(cursor).toString()
                val date2 = kotlinx.datetime.LocalDate.fromEpochDays(end).toString()
                calendar += try { gateway.calendar(date1, date2) } catch (e: HbmuSessionExpired) {
                    require(state.value.remember && activePassword.isNotBlank()) { "登录已过期，请重新登录" }
                    automaticLogin(state.value.account, activePassword)
                    gateway.calendar(date1, date2)
                }
                cursor = end
            }
        }
        val timedModel = HbmuCalendarParser.applyTimes(parsed.model, calendar)
        val tableId = "hbmu-${state.value.account}-$semester"
        database.withWriteTransaction {
            if (database.courseTableDao().getCourseTableById(tableId) == null) {
                database.courseTableDao().insert(CourseTable(tableId, "HBMU · $semester", Clock.System.now().toEpochMilliseconds()))
                timeSchedules.saveExclusiveTimeTable(
                    TimeTable(id = tableId, name = null, createdAt = Clock.System.now().toEpochMilliseconds(), defaultClassDuration = 40),
                    defaultTimeSlots.map { it.copy(timeTableId = tableId) }
                )
            }
            // Only migrate the old untouched fallback. User-edited slots and public bindings remain intact.
            val existingSlots = database.timeSlotDao().getTimeSlotsOnceByTimeTableId(tableId)
            val existingTimeTable = timeSchedules.getTimeTableById(tableId).first()
            val importModel = if (HbmuDefaultTimes.shouldReplaceLegacy(existingSlots)) {
                HbmuDefaultTimes.withCalibratedBaseline(timedModel)
            } else timedModel.copy(config = timedModel.config?.copy(
                defaultClassDuration = existingTimeTable?.defaultClassDuration ?: 40,
                defaultBreakDuration = existingTimeTable?.defaultBreakDuration ?: 10
            ))
            // Academic sync replaces lessons and baseline data, keeping any user's public/combo binding.
            conversion.importCourses(tableId, importModel.courses)
            conversion.importTimeSlots(tableId, importModel.timeSlots.orEmpty(),
                importModel.config?.defaultClassDuration, importModel.config?.defaultBreakDuration)
            importModel.config?.let { conversion.importCourseConfig(tableId, it) }
            val config = settings.getCourseConfigOnce(tableId) ?: CourseTableConfig(tableId)
            settings.insertOrUpdateCourseConfig(config.copy(showWeekends = parsed.hasWeekends))
        }
        val app = settings.getAppSettingsOnce()
        settings.insertOrUpdateAppSettings(app.copy(currentCourseTableId = tableId))
        state.update { it.copy(connected = true, signedIn = true, lastSync = Clock.System.now().toEpochMilliseconds(),
            recordCount = parsed.total, message = "已同步 ${parsed.total} 次排课及学校上课时间", captcha = null) }
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
        state.update { it.copy(account = "", connected = false, signedIn = false, displayName = "", captcha = null, requiresManualCaptcha = false, error = null, message = "已退出登录，本地课表仍可查看") }
    }

    private fun persist() {
        val current = state.value
        saved = HbmuSavedAccount(current.account, if (current.remember) activePassword else "", current.remember,
            current.autoSync, current.semester, current.lastSync, current.recordCount, current.displayName, current.signedIn)
        gateway.saveAccount(saved)
    }
}
