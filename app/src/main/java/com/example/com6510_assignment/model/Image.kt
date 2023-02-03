package com.example.com6510_assignment.model

import android.net.Uri
import java.util.Date

data class Image(
    val id: Int = 0,
    val imagePath: Uri,
    var title: String,
    var description: String? = null,
    var thumbnail: Uri? = null,
    var createDate: Date? = null) {

    override fun equals(other: Any?): Boolean {
        val is_equal = super.equals(other)

        if (!is_equal) { return false }
        val other_image = other as Image
        return this.imagePath == other_image.imagePath &&
                this.thumbnail == other_image.thumbnail &&
                this.description == other_image.description &&
                this.title == other_image.title &&
                this.id == other_image.id && this.createDate == other_image.createDate
    }
}