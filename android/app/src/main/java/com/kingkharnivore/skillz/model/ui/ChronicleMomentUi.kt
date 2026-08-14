package com.kingkharnivore.skillz.model.ui

sealed interface ChronicleMomentUi {
    val id: String
    val chronicleId: String
    val position: Int

    data class Text(
        override val id: String,
        override val chronicleId: String,
        override val position: Int,
        val text: String
    ) : ChronicleMomentUi

    data class Media(
        override val id: String,
        override val chronicleId: String,
        override val position: Int,
        val items: List<ChronicleMediaItemUi>
    ) : ChronicleMomentUi

    data class Voice(
        override val id: String,
        override val chronicleId: String,
        override val position: Int
    ) : ChronicleMomentUi

    data class Audio(
        override val id: String,
        override val chronicleId: String,
        override val position: Int
    ) : ChronicleMomentUi
}

data class ChronicleMediaItemUi(
    val id: String,
    val position: Int,
    val relativePath: String,
    val mimeType: String,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val isAvailable: Boolean
)
