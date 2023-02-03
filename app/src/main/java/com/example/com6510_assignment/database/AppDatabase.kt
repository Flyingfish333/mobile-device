package com.example.com6510_assignment.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ImageEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {

    abstract fun imageDao(): ImageDao

    companion object{
        private val DATABASE_NAME = "AssignmentDB"
        // For Singleton instantiation
        @Volatile private var db_instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return db_instance ?: synchronized(this) {
                db_instance ?: buildDatabase(context).also { db_instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                // Wipes and rebuilds instead of migrating if no Migration object specified.
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}