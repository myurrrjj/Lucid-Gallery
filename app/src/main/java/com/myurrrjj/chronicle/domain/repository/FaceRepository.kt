package com.myurrrjj.chronicle.domain.repository

import com.myurrrjj.chronicle.domain.model.PersonCluster
import kotlinx.coroutines.flow.Flow

interface FaceRepository {
    fun getAllClusters(): Flow<List<PersonCluster>>
    suspend fun getClusterById(clusterId: Long): PersonCluster?
    suspend fun updateClusterName(clusterId: Long, name: String)
    suspend fun getMediaIdsForCluster(clusterId: Long): List<Long>
    suspend fun deleteCluster(clusterId: Long)

}

