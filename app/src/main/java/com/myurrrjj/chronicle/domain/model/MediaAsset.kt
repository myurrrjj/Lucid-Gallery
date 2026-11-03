package com.myurrrjj.chronicle.domain.model

//@Parcelize
data class MediaAsset(
    val id: Long,
    val uriString: String,
    val name: String,
    val dateAdded: Long,
    val mimeType: String,
    val size: Long,
    val width: Int?,
    val height: Int?,
    val duration: Long?,
    val folderName: String?

) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
}