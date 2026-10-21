package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "topics")
data class TopicEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val description: String,

    val category: String,

    val difficulty: String,

    val numberOfCards: Int,

    @ColumnInfo(defaultValue = "0")
    val targetCardCount: Int = 0,

    val progress: Int = 0,

    val lastUpdated: Long = System.currentTimeMillis()
)
