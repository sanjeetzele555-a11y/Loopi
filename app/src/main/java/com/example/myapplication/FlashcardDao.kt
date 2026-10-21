package com.example.myapplication


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    @Query(
        "SELECT * FROM flashcards ORDER BY updatedAt DESC"
    )
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Insert
    suspend fun insertFlashcard(
        flashcard: FlashcardEntity
    ): Long

    @Query("SELECT COUNT(*) FROM flashcards WHERE topicId = :topicId")
    suspend fun getFlashcardCountForTopic(topicId: Int): Int

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getFlashcardById(id: Int): FlashcardEntity?

    @Update
    suspend fun updateFlashcard(
        flashcard: FlashcardEntity
    )

    @Query(
        "DELETE FROM flashcards WHERE id = :id"
    )
    suspend fun deleteFlashcardById(
        id: Int
    )

    @Query("DELETE FROM flashcards WHERE topicId = :topicId")
    suspend fun deleteFlashcardsForTopic(topicId: Int)
}
