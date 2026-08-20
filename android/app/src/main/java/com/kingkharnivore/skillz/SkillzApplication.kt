package com.kingkharnivore.skillz

import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.kingkharnivore.skillz.utils.localization.AppLocaleManager
import com.kingkharnivore.skillz.utils.user.UserPrefs
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCompletionProcessor
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class SkillzApplication : Application() {

    @Inject
    lateinit var userPrefs: UserPrefs

    @Inject lateinit var objectiveCompletionProcessor: ObjectiveCompletionProcessor
    @Inject lateinit var chronicleRepository: ChronicleRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        runBlocking {
            val savedTag = userPrefs.appLanguageTag.first()
            AppLocaleManager.applyLanguage(savedTag)
        }
        launchObjectiveReconciliation("startup")
        applicationScope.launch { runCatching { chronicleRepository.reconcileStorage() } }
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                launchObjectiveReconciliation("foreground")
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun launchObjectiveReconciliation(trigger: String) {
        applicationScope.launch {
            runCatching { objectiveCompletionProcessor.reconcileUnprocessedSessions() }
                .onFailure { error -> Log.e("ObjectiveReconciliation", "$trigger reconciliation failed", error) }
        }
    }
}
