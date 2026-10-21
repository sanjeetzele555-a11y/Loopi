package com.example.myapplication


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query("SELECT * FROM topics ORDER BY id ASC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(
        topicId: Int
    ): TopicEntity?

    @Query("SELECT * FROM topics WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun getTopicByTitle(title: String): TopicEntity?

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertTopic(
        topic: TopicEntity
    ): Long

    @Update
    suspend fun updateTopic(
        topic: TopicEntity
    )

    @Delete
    suspend fun deleteTopic(
        topic: TopicEntity
    )

    @Query("DELETE FROM topics WHERE id = :topicId")
    suspend fun deleteTopicById(
        topicId: Int
    )

    @Query("SELECT COUNT(*) FROM topics")
    suspend fun getTopicCount(): Int
}
