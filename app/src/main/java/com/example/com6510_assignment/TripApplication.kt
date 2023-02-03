package com.example.com6510_assignment

import android.app.Application
import com.example.com6510_assignment.database.AppDatabase
import com.example.com6510_assignment.repository.ImageRepository

class TripApplication: Application() {
    // This has been updated to initialize the repository along with the database when the
    // using by lazy again,which ensures either are created until they are
    // needed (i.e. referenced for the first time).
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: ImageRepository by lazy { ImageRepository(database.imageDao()) }
}