package com.kingkharnivore.skillz.localization

import androidx.annotation.StringRes
import com.kingkharnivore.skillz.R

enum class AppLanguage(
    val tag: String?,
    @StringRes val labelRes: Int
) {
    SYSTEM(null, R.string.help_language_system_default),
    ENGLISH("en", R.string.help_language_english),
    SPANISH("es", R.string.help_language_spanish),
    HINDI("hi", R.string.help_language_hindi),
    MARATHI("mr", R.string.help_language_marathi);

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag == tag } ?: SYSTEM
        }
    }
}