package com.example.myapplication


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "review_results",
    indices = [
        Index(value = ["topicId"]),
        Index(value = ["flashcardId"])
    ]
)
data class ReviewResultEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val topicId: Int,

    val flashcardId: Int,

    val question: String,

    val correctAnswer: String,

    @ColumnInfo(defaultValue = "")
    val explanation: String = "",

    val userAnswer: String,

    val isCorrect: Boolean,

    val confidence: String,

    @ColumnInfo(defaultValue = "0")
    val responseTimeMillis: Long = 0L,

    val reviewedAt: Long =
        System.currentTimeMillis()
)
