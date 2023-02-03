package com.example.com6510_assignment.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    // suspend keyword not needed with the use of Flow
    @Query("Select * from image ORDER by id ASC")
    fun getImages(): Flow<List<ImageEntity>>

    // Useful for tracking Entities
    @Query("Select * from image Where id = :id")
    fun getImage(id: Int): Flow<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(imageEntity: ImageEntity): Long

    @Update
    suspend fun update(imageEntity: ImageEntity)

    @Delete
    suspend fun delete(imageEntity: ImageEntity)

}