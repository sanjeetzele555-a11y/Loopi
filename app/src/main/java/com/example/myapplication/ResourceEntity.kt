package com.example.myapplication

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "resources",
    indices = [
        Index(value = ["topicId"])
    ]
)
data class ResourceEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val topicId: Int,

    val type: String,

    val title: String,

    val content: String = "",

    val url: String = "",

    val fileUri: String = "",

    val description: String = "",

    val tags: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
