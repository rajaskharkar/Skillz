package com.kingkharnivore.skillz

import android.app.Application
import com.kingkharnivore.skillz.utils.localization.AppLocaleManager
import com.kingkharnivore.skillz.utils.user.UserPrefs
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class SkillzApplication : Application() {

    @Inject
    lateinit var userPrefs: UserPrefs

    override fun onCreate() {
        super.onCreate()

        runBlocking {
            val savedTag = userPrefs.appLanguageTag.first()
            AppLocaleManager.applyLanguage(savedTag)
        }
    }
}