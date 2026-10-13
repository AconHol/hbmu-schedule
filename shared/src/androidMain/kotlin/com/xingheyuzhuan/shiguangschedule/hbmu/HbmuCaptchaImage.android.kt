package com.xingheyuzhuan.shiguangschedule.hbmu

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
actual fun HbmuCaptchaImage(bytes: ByteArray, modifier: Modifier) {
    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
    bitmap?.let { Image(it, "教务验证码", modifier, contentScale = ContentScale.Fit) }
}
