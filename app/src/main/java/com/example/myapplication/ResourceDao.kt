package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Query(
        "SELECT * FROM resources ORDER BY updatedAt DESC"
    )
    fun getAllResources(): Flow<List<ResourceEntity>>

    @Insert
    suspend fun insertResource(
        resource: ResourceEntity
    ): Long

    @Update
    suspend fun updateResource(
        resource: ResourceEntity
    )

    @Query(
        "DELETE FROM resources WHERE id = :id"
    )
    suspend fun deleteResourceById(
        id: Int
    )

    @Query("DELETE FROM resources WHERE topicId = :topicId")
    suspend fun deleteResourcesForTopic(topicId: Int)
}
