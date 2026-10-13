// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.hbmu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.xingheyuzhuan.shiguangschedule.tool.SecureCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.koin.core.context.GlobalContext
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.FloatBuffer
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

actual fun createHbmuGateway(): HbmuGateway = AndroidHbmuGateway(GlobalContext.get().get())

internal class AndroidHbmuGateway(private val context: Context) : HbmuGateway {
    private val base = "https://jw.hbmu.edu.cn"
    private val uri = URI(base)
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    private val prefs = context.getSharedPreferences("hbmu_secure", Context.MODE_PRIVATE)
    private val crypto = SecureCrypto()
    private val json = Json { ignoreUnknownKeys = true }
    private val ocr by lazy { HbmuOfflineOcr(context) }
    private var studentName = ""

    @Serializable
    private data class StoredCookie(val name: String, val value: String, val path: String?, val secure: Boolean, val expires: Long)

    init {
        runCatching {
            val stored = json.decodeFromString<List<StoredCookie>>(readEncrypted("cookies") ?: "[]")
            stored.filter { it.expires < 0 || it.expires > System.currentTimeMillis() }.forEach {
                cookies.cookieStore.add(uri, HttpCookie(it.name, it.value).apply {
                    domain = uri.host; path = it.path ?: "/"; secure = it.secure
                    maxAge = if (it.expires < 0) -1 else (it.expires - System.currentTimeMillis()) / 1000
                })
            }
        }
    }

    private fun readEncrypted(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return crypto.decrypt(encrypted, prefs.getString("${key}_iv", "") ?: "")
    }

    private fun writeEncrypted(key: String, value: String) {
        val result = crypto.encrypt(value) ?: error("本机加密保存失败，请关闭保持登录后重试")
        check(prefs.edit().putString(key, result.encryptedData).putString("${key}_iv", result.iv).commit()) {
            "保存登录状态失败"
        }
    }

    override fun loadAccount(): HbmuSavedAccount = runCatching {
        json.decodeFromString<HbmuSavedAccount>(readEncrypted("account") ?: "{}")
    }.getOrDefault(HbmuSavedAccount())

    override fun saveAccount(account: HbmuSavedAccount) {
        writeEncrypted("account", json.encodeToString(account))
        if (!account.remember) prefs.edit().remove("cookies").remove("cookies_iv").apply()
    }

    override fun clearSession() {
        cookies.cookieStore.removeAll()
        prefs.edit().remove("cookies").remove("cookies_iv").apply()
    }

    private fun persistCookies() {
        if (!loadAccount().remember) return
        val saved = cookies.cookieStore.cookies.filterNot { it.hasExpired() }.map {
            StoredCookie(it.name, it.value, it.path, it.secure,
                if (it.maxAge < 0) -1 else System.currentTimeMillis() + it.maxAge * 1000)
        }
        writeEncrypted("cookies", json.encodeToString(saved))
    }

    private fun request(path: String, form: Map<String, String>? = null): ByteArray {
        var target = URI(base).resolve(path)
        repeat(6) {
            require(target.scheme == "https" && target.host == uri.host) { "教务跳转地址异常" }
            val connection = target.toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 20000
                connection.readTimeout = 25000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "HBMU-Schedule/2.1 Android")
                connection.setRequestProperty("Referer", "$base/")
                cookies.get(target, emptyMap()).forEach { (name, values) -> connection.setRequestProperty(name, values.joinToString("; ")) }
                if (form != null && it == 0) {
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    val body = form.entries.joinToString("&") { (key, value) ->
                        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}" }
                    connection.outputStream.use { stream -> stream.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val headers = connection.headerFields.filterKeys { it != null }.mapKeys { it.key!! }
                cookies.put(target, headers)
                persistCookies()
                if (status in 300..399) {
                    target = target.resolve(connection.getHeaderField("Location") ?: error("教务跳转异常"))
                } else {
                    require(status in 200..299) { "教务暂时无法访问（HTTP $status）" }
                    return connection.inputStream.use { stream -> stream.readBytes() }
                }
            } finally { connection.disconnect() }
        }
        error("教务跳转次数过多")
    }

    override suspend fun captcha(): HbmuCaptcha = withContext(Dispatchers.IO) {
        request("/") // establish the same server-side session as the captcha and login
        val bytes = request("/yzm?d=${System.currentTimeMillis()}")
        val recognized = runCatching { ocr.recognize(bytes) }.getOrDefault("")
        HbmuCaptcha(bytes, recognized)
    }

    override suspend fun login(account: String, password: String, captcha: String) = withContext(Dispatchers.IO) {
        require(captcha.matches(Regex("[A-Za-z0-9]{4}"))) { "验证码应为4位数字或字母" }
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(captcha.repeat(4).toByteArray(Charsets.UTF_8), "AES"))
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
        val response = request("/new/login", mapOf("account" to account, "pwd" to encrypted, "verifycode" to captcha)).toString(Charsets.UTF_8)
        val root = runCatching { json.parseToJsonElement(response).jsonObject }.getOrElse { error("教务登录响应异常") }
        val code = root["code"]?.jsonPrimitive?.intOrNull ?: error("教务登录响应缺少状态码")
        if (code < 0) {
            val message = (root["msg"] ?: root["message"])?.jsonPrimitive?.contentOrNull ?: "登录失败，请检查学号和密码"
            throw HbmuLoginException(message, message.contains("验证码") || message.contains("校验码"))
        }
        root["data"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { request(it) }
        Unit
    }

    override suspend fun terms(): List<HbmuTerm> = withContext(Dispatchers.IO) {
        val html = request("/xsgrkbcx!getXsgrbkList.action").toString(Charsets.UTF_8)
        if (html.contains("id=\"login-form\"") || html.contains("/new/login")) throw HbmuSessionExpired()
        studentName = HbmuProfileParser.nameFromHtml(html)
        HbmuTermParser.parse(html)
    }

    override fun displayName(): String = studentName

    override suspend fun courses(semester: String, rows: Int): String = withContext(Dispatchers.IO) {
        require(semester.matches(Regex("[0-9]{6}")) && rows in 1..10000)
        val body = request("/xsgrkbcx!getDataList.action?xnxqdm=$semester&zc=&page=1&rows=$rows&sort=kxh&order=asc").toString(Charsets.UTF_8)
        if (body.trimStart().startsWith("<")) throw HbmuSessionExpired()
        body
    }
}

/** Restricted ddddocr CTC head + INT8 weights in ORT format. Inference stays on the device. */
internal class HbmuOfflineOcr(context: Context) {
    private val environment = OrtEnvironment.getEnvironment()
    private val charset = Json.parseToJsonElement(context.assets.open("hbmu/charset.json").bufferedReader().use { it.readText() })
        .jsonArray.map { it.jsonPrimitive.content }
    private val session = OrtSession.SessionOptions().use { options ->
        options.setIntraOpNumThreads(2)
        options.addConfigEntry("session.load_model_format", "ORT")
        environment.createSession(context.assets.open("hbmu/captcha.ort").use { it.readBytes() }, options)
    }

    fun recognize(bytes: ByteArray): String {
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return ""
        val width = (original.width * 64 / original.height).coerceIn(16, 1024)
        val bitmap = Bitmap.createScaledBitmap(original, width, 64, true)
        val pixels = IntArray(width * 64)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, 64)
        val values = FloatArray(pixels.size) { i ->
            val pixel = pixels[i]
            (0.299f * ((pixel shr 16) and 255) + 0.587f * ((pixel shr 8) and 255) + 0.114f * (pixel and 255)) / 255f
        }
        if (bitmap !== original) bitmap.recycle()
        original.recycle()
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(values), longArrayOf(1, 1, 64, width.toLong())).use { input ->
            session.run(mapOf(session.inputNames.first() to input)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val output = result[0].value as Array<Array<FloatArray>>
                val steps = if (output.size == 1) output[0].toList() else output.map { it[0] }
                var previous = -1
                return buildString {
                    steps.forEach { scores ->
                        val index = scores.indices.maxByOrNull { scores[it] } ?: 0
                        if (index != 0 && index != previous) append(charset.getOrElse(index) { "" })
                        previous = index
                    }
                }.trim()
            }
        }
    }
}
