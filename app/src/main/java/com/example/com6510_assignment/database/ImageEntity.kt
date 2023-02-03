package com.example.com6510_assignment.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "image", indices=[Index(value=["id", "image_title"])])
data class ImageEntity (
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name="image_path") val imagePath: String,
    @ColumnInfo(name="image_title") var title: String,
    @ColumnInfo(name="image_description") var description: String?,
    @ColumnInfo(name="thumbnail_filename") var thumbnail: String?,
    @ColumnInfo(name="create_date") var createDate: Date?)