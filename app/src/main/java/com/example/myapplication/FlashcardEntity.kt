package com.example.myapplication



import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "flashcards",
    indices = [
        Index(value = ["topicId"])
    ]
)
data class FlashcardEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val topicId: Int,

    val question: String,

    val answer: String,

    @ColumnInfo(defaultValue = "")
    val explanation: String = "",

    /** Wrong options kept with the card (one per line), e.g. from PDF-generated questions. */
    @ColumnInfo(defaultValue = "")
    val wrongOptions: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
