package com.kingkharnivore.skillz.ui.screen.chronicle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bounded process-wide thumbnail cache; durable originals remain the authority. */
internal object ChronicleThumbnailLoader {
    private val cache = object : LruCache<String, Bitmap>(CACHE_KIB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    suspend fun load(file: File, mimeType: String): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        val key = "${file.path}:${file.lastModified()}:${file.length()}"
        synchronized(cache) { cache.get(key) }?.let { return@withContext it }
        val bitmap = if (mimeType.startsWith("video/")) {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(file.path)
                    retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            }.getOrNull()
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > TARGET_PX * 2 || bounds.outHeight / sample > TARGET_PX * 2) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        bitmap?.also { synchronized(cache) { cache.put(key, it) } }
    }

    private const val TARGET_PX = 720
    private const val CACHE_KIB = 24 * 1024
}
