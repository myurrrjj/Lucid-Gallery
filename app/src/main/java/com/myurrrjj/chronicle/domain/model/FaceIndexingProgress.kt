package com.myurrrjj.chronicle.domain.model

enum class FaceIndexingStatus {
    IDLE, QUEUED, RUNNING, PAUSING, PAUSED, COMPLETED, FAILED
}

enum class FaceIndexingSchedule(val label: String, val description: String) {
    MANUAL_ONLY(
        label = "Only when I start it",
        description = "Indexing runs only when you tap Start. It never resumes on its own."
    ),
    ON_APP_OPEN(
        label = "Resume when I open the app",
        description = "If photos are still waiting, indexing picks up automatically when Chronicle opens."
    ),
    WHILE_CHARGING(
        label = "Only while charging",
        description = "Starts when you plug in. Android pauses it if you unplug, and resumes when you plug back in."
    ),
    WHEN_IDLE(
        label = "Overnight (idle + charging)",
        description = "Runs while the phone is charging and unused — the gentlest option for large libraries."
    )

}


data class FaceIndexingOptions(
    val schedule: FaceIndexingSchedule = FaceIndexingSchedule.MANUAL_ONLY,
    val pauseOnLowBattery: Boolean = true,
    val pauseOnLowStorage: Boolean = true
)

data class FaceIndexingProgress(
    val status: FaceIndexingStatus = FaceIndexingStatus.IDLE,
    val processed: Int = 0,
    val total: Int = 0,
    val facesFound: Int = 0,
    val currentItem: String? = null,
    val message: String? = null


) {
    val fraction: Float
        get() = if (total <= 0) 0f else (processed.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    val percentage: Float
        get() = (fraction * 100f)

    val pending: Int
        get() = (total - processed).coerceAtLeast(0)


    val isBusy: Boolean
        get() = status == FaceIndexingStatus.PAUSING || status == FaceIndexingStatus.RUNNING || status == FaceIndexingStatus.QUEUED

    val canPause: Boolean
        get() = status == FaceIndexingStatus.RUNNING || status == FaceIndexingStatus.QUEUED

    val canResume: Boolean
        get() = (status == FaceIndexingStatus.PAUSED || status == FaceIndexingStatus.FAILED) && pending > 0

    val canStart: Boolean
        get() = (status == FaceIndexingStatus.IDLE || status == FaceIndexingStatus.COMPLETED) && pending > 0


    val isVisible: Boolean
        get() = status != FaceIndexingStatus.IDLE || processed > 0 || status == FaceIndexingStatus.FAILED
}