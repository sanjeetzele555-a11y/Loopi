package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPackDao {
    @Query("SELECT * FROM study_packs ORDER BY updatedAt DESC, name COLLATE NOCASE ASC")
    fun getAllStudyPacks(): Flow<List<StudyPackEntity>>

    @Query("SELECT * FROM study_packs WHERE id = :packId LIMIT 1")
    suspend fun getStudyPackById(packId: Int): StudyPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPack(pack: StudyPackEntity): Long

    @Update
    suspend fun updateStudyPack(pack: StudyPackEntity)

    @Query("DELETE FROM study_packs WHERE id = :packId")
    suspend fun deleteStudyPackById(packId: Int)

}
