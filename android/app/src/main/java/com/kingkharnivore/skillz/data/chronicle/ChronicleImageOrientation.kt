package com.kingkharnivore.skillz.data.chronicle

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/** Lossless EXIF orientation support shared by capture review and Chronicle thumbnails. */
internal data class ChronicleImageTransform(
    val rotationDegrees: Int,
    val flippedHorizontally: Boolean,
)

internal fun readChronicleImageTransform(file: File): ChronicleImageTransform = runCatching {
    ExifInterface(file).let { exif ->
        ChronicleImageTransform(
            rotationDegrees = normalizeChronicleRotationDegrees(exif.rotationDegrees),
            flippedHorizontally = exif.isFlipped,
        )
    }
}.getOrDefault(ChronicleImageTransform(0, false))

internal fun persistChronicleQuarterTurns(file: File, quarterTurns: Int) {
    val normalized = normalizeChronicleQuarterTurns(quarterTurns)
    if (normalized == 0) return
    ExifInterface(file).apply {
        if (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED) ==
            ExifInterface.ORIENTATION_UNDEFINED
        ) {
            resetOrientation()
        }
        rotate(normalized * 90)
        saveAttributes()
    }
}

internal fun transformChronicleBitmap(
    bitmap: Bitmap,
    transform: ChronicleImageTransform,
): Bitmap {
    if (!transform.flippedHorizontally && transform.rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply {
        // ExifInterface defines flipped orientations as a horizontal flip first,
        // followed by the reported clockwise rotation.
        if (transform.flippedHorizontally) postScale(-1f, 1f)
        if (transform.rotationDegrees != 0) postRotate(transform.rotationDegrees.toFloat())
    }
    val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (transformed !== bitmap) bitmap.recycle()
    return transformed
}

internal fun orientedChronicleDimensions(file: File, width: Int, height: Int): Pair<Int, Int> =
    if (readChronicleImageTransform(file).rotationDegrees % 180 != 0) height to width
    else width to height

internal fun normalizeChronicleQuarterTurns(value: Int): Int = when (((value % 4) + 4) % 4) {
    0 -> 0
    1 -> 1
    2 -> 2
    else -> -1
}

internal fun normalizeChronicleRotationDegrees(value: Int): Int = ((value % 360) + 360) % 360
