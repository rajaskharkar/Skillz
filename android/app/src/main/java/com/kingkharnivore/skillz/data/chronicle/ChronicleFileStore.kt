package com.kingkharnivore.skillz.data.chronicle

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Durable, app-owned storage shared by all Chronicle binary Moment types. */
@Singleton
class ChronicleFileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class StoredFile(
        val relativePath: String,
        val mimeType: String,
        val size: Long,
        val durationMs: Long? = null,
        val width: Int? = null,
        val height: Int? = null
    )

    private val durableRoot get() = File(context.filesDir, "chronicle")
    private val stagingRoot get() = File(context.cacheDir, "chronicle_staging")
    private val captureRoot get() = File(context.cacheDir, "chronicle_capture")

    fun createCaptureOutput(video: Boolean): Uri {
        captureRoot.mkdirsOrThrow()
        val extension = if (video) ".mp4" else ".jpg"
        val file = File(captureRoot, UUID.randomUUID().toString() + extension)
        if (!file.createNewFile()) throw IOException("Unable to create capture output")
        return FileProvider.getUriForFile(context, "${context.packageName}.chronicle-files", file)
    }

    suspend fun discardCapture(uri: Uri) = withContext(Dispatchers.IO) {
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: return@withContext
        requireSafeSegment(name)
        File(captureRoot, name).delete()
    }

    suspend fun importMedia(
        chronicleId: String,
        source: Uri,
        operationId: String = UUID.randomUUID().toString()
    ): StoredFile = withContext(Dispatchers.IO) {
        requireSafeSegment(chronicleId)
        requireSafeSegment(operationId)
        val resolver = context.contentResolver
        val mime = resolver.getType(source)?.lowercase()
            ?: throw IOException("Media type is unavailable")
        require(mime.startsWith("image/") || mime.startsWith("video/"))
        val operation = File(stagingRoot, operationId).also { it.mkdirsOrThrow() }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        val identity = UUID.randomUUID().toString() + extension?.let { ".$it" }.orEmpty()
        val staged = File(operation, identity)
        var destination: File? = null
        try {
            resolver.openInputStream(source)?.use { input ->
                staged.outputStream().buffered(COPY_BUFFER_BYTES).use { output -> input.copyTo(output, COPY_BUFFER_BYTES) }
            } ?: throw IOException("Media source is unavailable")
            validate(staged, mime)
            val stagedSize = staged.length()
            val destinationDir = File(durableRoot, "$chronicleId/media").also { it.mkdirsOrThrow() }
            val finalized = File(destinationDir, identity)
            destination = finalized
            if (!staged.renameTo(finalized)) {
                staged.inputStream().use { input ->
                    finalized.outputStream().buffered(COPY_BUFFER_BYTES).use { output -> input.copyTo(output, COPY_BUFFER_BYTES) }
                }
                staged.delete()
            }
            if (finalized.length() != stagedSize) {
                throw IOException("Media copy is incomplete")
            }
            val metadata = metadata(finalized, mime)
            StoredFile("chronicle/$chronicleId/media/$identity", mime, finalized.length(),
                metadata.first, metadata.second, metadata.third)
        } catch (failure: Exception) {
            destination?.delete()
            throw failure
        } finally {
            operation.deleteRecursively()
        }
    }

    fun resolve(relativePath: String): File? {
        val candidate = File(context.filesDir, relativePath)
        val root = durableRoot.canonicalFile
        val canonical = runCatching { candidate.canonicalFile }.getOrNull() ?: return null
        return canonical.takeIf { it.path.startsWith(root.path + File.separator) }
    }

    suspend fun deleteIfOwned(relativePath: String) = withContext(Dispatchers.IO) {
        resolve(relativePath)?.delete()
    }

    suspend fun deleteChronicle(chronicleId: String) = withContext(Dispatchers.IO) {
        requireSafeSegment(chronicleId)
        File(durableRoot, chronicleId).deleteRecursively()
    }

    suspend fun reconcileStaging(olderThanMs: Long = STALE_STAGING_MS) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        stagingRoot.listFiles().orEmpty().filter { it.lastModified() < cutoff }.forEach(File::deleteRecursively)
        captureRoot.listFiles().orEmpty().filter { it.lastModified() < cutoff }.forEach(File::delete)
    }

    private fun validate(file: File, mime: String) {
        if (!file.isFile || file.length() <= 0L) throw IOException("Media is empty")
        val valid = if (mime.startsWith("image/")) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            bounds.outWidth > 0 && bounds.outHeight > 0
        } else {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(file.path)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) != null
                }
            }.getOrDefault(false)
        }
        if (!valid) throw IOException("Media is corrupt or unsupported")
    }

    private fun metadata(file: File, mime: String): Triple<Long?, Int?, Int?> =
        if (mime.startsWith("image/")) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            Triple(null, bounds.outWidth.takeIf { it > 0 }, bounds.outHeight.takeIf { it > 0 })
        } else runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(file.path)
                Triple(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                )
            }
        }.getOrDefault(Triple(null, null, null))

    private fun requireSafeSegment(value: String) {
        require(value.isNotBlank() && value != "." && value != ".." && '/' !in value && '\\' !in value)
    }

    private fun File.mkdirsOrThrow() {
        if (!isDirectory && !mkdirs()) throw IOException("Unable to create Chronicle storage")
    }

    private companion object {
        const val COPY_BUFFER_BYTES = 64 * 1024
        const val STALE_STAGING_MS = 24 * 60 * 60 * 1000L
    }
}
