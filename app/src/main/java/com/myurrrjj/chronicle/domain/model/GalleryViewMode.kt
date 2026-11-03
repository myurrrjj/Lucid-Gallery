package com.myurrrjj.chronicle.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class GalleryViewMode(
    val minCellSizeDp: Int,
    val zoomThreshold: Float
) {

    DAY(minCellSizeDp = 120, zoomThreshold = 1.5f),

    MONTH(minCellSizeDp = 70, zoomThreshold = 1.0f),

    YEAR(minCellSizeDp = 40, zoomThreshold = 0.5f);

    fun formatHeader(timestampSeconds: Long): String {
        val date = Instant.ofEpochSecond(timestampSeconds).atZone(ZoneId.systemDefault())
        return when (this) {
            DAY -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            YEAR -> date.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }
    fun getGroupingKey(timestampSeconds: Long):String{
        val date = Instant.ofEpochSecond(timestampSeconds).atZone(ZoneId.systemDefault())
        return when (this) {
            DAY -> date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            MONTH -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            YEAR -> date.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }
}
