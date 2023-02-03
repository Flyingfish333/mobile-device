package com.example.com6510_assignment

import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.com6510_assignment.viewmodel.ImageViewModel
import com.example.com6510_assignment.viewmodel.ImageViewModelFactory

open class TripAppCompatActivity: AppCompatActivity() {
    // Instantiate the ViewModel from the ImageViewModelFactory
    // which extends ViewModelProvider.Factory
    protected val imageViewModel: ImageViewModel by viewModels {
        ImageViewModelFactory((application as TripApplication).repository, application)
    }
}