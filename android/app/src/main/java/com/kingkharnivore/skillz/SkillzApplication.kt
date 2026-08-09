package com.kingkharnivore.skillz

import android.app.Application
import com.kingkharnivore.skillz.utils.localization.AppLocaleManager
import com.kingkharnivore.skillz.utils.user.UserPrefs
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCompletionProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class SkillzApplication : Application() {

    @Inject
    lateinit var userPrefs: UserPrefs

    @Inject lateinit var objectiveCompletionProcessor: ObjectiveCompletionProcessor
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        runBlocking {
            val savedTag = userPrefs.appLanguageTag.first()
            AppLocaleManager.applyLanguage(savedTag)
        }
        applicationScope.launch { objectiveCompletionProcessor.reconcileUnprocessedSessions() }
    }
}
