package com.myurrrjj.chronicle.data.local.projection

import androidx.room.Embedded
import com.myurrrjj.chronicle.data.local.entity.FaceClusterEntity

data class PersonClusterSummary(
    @Embedded val cluster: FaceClusterEntity,
    val coverMediaId: Long?,
    val coverFaceIndex: Int?,
    val mediaCount: Int)