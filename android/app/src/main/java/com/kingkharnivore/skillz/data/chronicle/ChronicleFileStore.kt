package com.kingkharnivore.skillz.data.chronicle

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
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
    data class StoredFile(val relativePath: String, val mimeType: String, val size: Long)

    private val durableRoot get() = File(context.filesDir, "chronicle")
    private val stagingRoot get() = File(context.cacheDir, "chronicle_staging")

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
        try {
            resolver.openInputStream(source)?.use { input ->
                staged.outputStream().buffered(COPY_BUFFER_BYTES).use { output -> input.copyTo(output, COPY_BUFFER_BYTES) }
            } ?: throw IOException("Media source is unavailable")
            validate(staged, mime)
            val destinationDir = File(durableRoot, "$chronicleId/media").also { it.mkdirsOrThrow() }
            val destination = File(destinationDir, identity)
            if (!staged.renameTo(destination)) {
                staged.inputStream().use { input ->
                    destination.outputStream().buffered(COPY_BUFFER_BYTES).use { output -> input.copyTo(output, COPY_BUFFER_BYTES) }
                }
                staged.delete()
            }
            StoredFile("chronicle/$chronicleId/media/$identity", mime, destination.length())
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
