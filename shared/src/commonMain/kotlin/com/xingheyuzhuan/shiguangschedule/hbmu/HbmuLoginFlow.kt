package com.xingheyuzhuan.shiguangschedule.hbmu

/** Retry captcha failures only. Credential and network failures must stop immediately. */
internal class HbmuLoginFlow(private val gateway: HbmuGateway) {
    suspend fun automatic(
        account: String,
        password: String,
        onAttempt: (Int) -> Unit,
        onCaptcha: (HbmuCaptcha, Boolean) -> Unit
    ) {
        var lastCaptcha = HbmuCaptcha(byteArrayOf(), "")
        repeat(3) { index ->
            onAttempt(index + 1)
            val captcha = gateway.captcha()
            lastCaptcha = captcha
            onCaptcha(captcha, false)
            if (captcha.recognized.matches(Regex("[0-9a-z]{4}"))) {
                try {
                    gateway.login(account, password, captcha.recognized)
                    return
                } catch (e: HbmuLoginException) {
                    if (!e.captchaError) throw e
                }
            }
        }
        // A rejected image may be invalidated by the server. Show a fresh, unfilled image.
        onCaptcha(lastCaptcha.copy(recognized = ""), true)
        onCaptcha(gateway.captcha().copy(recognized = ""), true)
        throw HbmuLoginException("连续三次自动识别未通过，请填写下面的新验证码", true)
    }
}
