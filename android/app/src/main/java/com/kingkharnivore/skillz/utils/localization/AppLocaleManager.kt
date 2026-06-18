package com.kingkharnivore.skillz.utils.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocaleManager {

    fun applyLanguage(tag: String?) {
        val locales = if (tag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}