package com.kingkharnivore.skillz

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.ui.navigation.SkillzNavHost
import com.kingkharnivore.skillz.ui.service.AliveFlowServiceController
import com.kingkharnivore.skillz.ui.skills.NotificationPermissionGate
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var aliveFlowRepository: AliveFlowRepository
    @Inject lateinit var aliveFlowServiceController: AliveFlowServiceController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // cold start reinstate
        maybeReinstateFlowNotification()

        setContent {
            SkillzTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SkillzApp(
                        onPermissionGranted = { maybeReinstateFlowNotification() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        maybeReinstateFlowNotification()
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
    onPermissionGranted: () -> Unit
) {
    val navController = rememberNavController()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NotificationPermissionGate(onGranted = onPermissionGranted)
        SkillzNavHost(navController = navController)
    }
}
