package com.kingkharnivore.skillz.domain.reminders

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kingkharnivore.skillz.MainActivity

private const val OPEN_REMINDER = "com.kingkharnivore.skillz.OPEN_ACTIVITY_REMINDER"

fun reminderLaunchIntent(context: Context, activityId: Long): Intent =
    Intent(context, MainActivity::class.java)
        .setAction(OPEN_REMINDER)
        .setData(Uri.parse("scyra://open-reminder/$activityId"))
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

fun Intent.reminderActivityId(): Long? {
    if (action != OPEN_REMINDER || data?.scheme != "scyra" || data?.host != "open-reminder") return null
    return data?.lastPathSegment?.toLongOrNull()?.takeIf { it > 0 }
}
