package com.kingkharnivore.skillz

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.kingkharnivore.skillz.domain.reminders.reminderActivityId
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.utils.health.HealthRefreshUseCase
import com.kingkharnivore.skillz.ui.navigation.SkillzNavHost
import com.kingkharnivore.skillz.ui.service.AliveFlowServiceController
import com.kingkharnivore.skillz.ui.screen.NotificationPermissionGate
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var aliveFlowRepository: AliveFlowRepository

    @Inject
    lateinit var aliveFlowServiceController: AliveFlowServiceController

    @Inject
    lateinit var healthRefreshUseCase: HealthRefreshUseCase

    private val reminderRequest = mutableStateOf<Long?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reminderRequest.value = intent.reminderActivityId()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        reminderRequest.value?.let { outState.putLong("reminderRequest", it) }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reminderRequest.value = if (savedInstanceState != null) {
            savedInstanceState.getLong("reminderRequest").takeIf { it > 0 }
        } else intent.reminderActivityId()

        maybeReinstateFlowNotification()

        setContent {
            SkillzTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SkillzApp(
                        onPermissionGranted = { maybeReinstateFlowNotification() },
                        reminderActivityId = reminderRequest.value,
                        onReminderHandled = { reminderRequest.value = null }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        maybeReinstateFlowNotification()
        lifecycleScope.launch { runCatching { healthRefreshUseCase.refreshForeground() } }
    }

    private fun maybeReinstateFlowNotification() {
        lifecycleScope.launch {
            val entity = aliveFlowRepository.getOngoingSession().firstOrNull()
            val shouldShow = entity != null && (entity.isInFlowMode || entity.isRunning)
            if (!shouldShow) return@launch

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return@launch
            }

            aliveFlowServiceController.start()
        }
    }
}

@Composable
fun SkillzApp(
    onPermissionGranted: () -> Unit,
    reminderActivityId: Long? = null,
    onReminderHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NotificationPermissionGate(onGranted = onPermissionGranted)
        SkillzNavHost(navController = navController, reminderActivityId = reminderActivityId, onReminderHandled = onReminderHandled)
    }
}