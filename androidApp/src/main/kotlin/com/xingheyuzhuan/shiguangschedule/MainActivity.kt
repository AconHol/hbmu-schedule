// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.service.CourseNotificationWorker
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.xingheyuzhuan.shiguangschedule.hbmu.HbmuRepository
import com.xingheyuzhuan.shiguangschedule.tool.ExternalFileManager

class MainActivity : AppCompatActivity() {
    private val hbmu: HbmuRepository by inject()
    private val settings: AppSettingsRepository by inject()
    private var scheduleOpenRequest by mutableIntStateOf(0)
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) lifecycleScope.launch { enableInitialReminders() }
    }

    private suspend fun enableInitialReminders() {
        val prefs = getSharedPreferences("hbmu_notification_onboarding", MODE_PRIVATE)
        if (!prefs.getBoolean("initialized", false)) {
            val current = settings.getAppSettingsOnce()
            settings.insertOrUpdateAppSettings(current.copy(reminderEnabled = true, schedulePreviewEnabled = true))
            prefs.edit().putBoolean("initialized", true).apply()
        }
        CourseNotificationWorker.enqueue(this)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { hbmu.resume() }
        CourseNotificationWorker.enqueue(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                hbmu.uiState.collect { state ->
                    val prefs = getSharedPreferences("hbmu_notification_onboarding", MODE_PRIVATE)
                    if (state.signedIn && state.lastSync > 0 && !prefs.getBoolean("requested", false)) {
                        prefs.edit().putBoolean("requested", true).apply()
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else enableInitialReminders()
                    }
                }
            }
        }

        setContent {
            App(scheduleOpenRequest = scheduleOpenRequest)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("open_week_schedule", false)) scheduleOpenRequest++
        val action = intent.action
        val uri: Uri? = when (action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }

        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val fileName = getFileName(uri) ?: "import.json"
                    ExternalFileManager.onFileReceived(fileName, bytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }
}
