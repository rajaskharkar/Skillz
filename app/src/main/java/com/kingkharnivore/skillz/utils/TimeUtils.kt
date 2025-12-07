package com.kingkharnivore.skillz.utils

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format("%d hr %02d min %02d sec", hours, minutes, seconds)
        minutes > 0 -> String.format("%d min %02d sec", minutes, seconds)
        else -> String.format("%d sec", seconds)
    }
}