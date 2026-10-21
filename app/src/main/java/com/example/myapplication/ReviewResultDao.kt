package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewResultDao {

    @Query(
        """
        SELECT * FROM review_results
        ORDER BY reviewedAt DESC
        """
    )
    fun getAllResults():
            Flow<List<ReviewResultEntity>>

    @Query("SELECT COUNT(*) FROM review_results WHERE topicId = :topicId")
    suspend fun getAnswerCountForTopic(topicId: Int): Int

    @Query("SELECT COUNT(*) FROM review_results WHERE topicId = :topicId AND isCorrect = 1")
    suspend fun getCorrectAnswerCountForTopic(topicId: Int): Int

    @Query(
        """
        SELECT * FROM review_results
        WHERE topicId = :topicId
        ORDER BY reviewedAt DESC
        """
    )
    fun getResultsForTopic(
        topicId: Int
    ): Flow<List<ReviewResultEntity>>

    @Insert
    suspend fun insertResult(
        result: ReviewResultEntity
    ): Long

    @Query(
        """
        DELETE FROM review_results
        WHERE topicId = :topicId
        """
    )
    suspend fun clearTopicResults(
        topicId: Int
    )
}

