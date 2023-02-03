package com.example.com6510_assignment.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Size
import com.example.com6510_assignment.model.Image
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

/**
 * Returns the Uri object that points to a thumbnail file if it exits
 * otherwise creates it before returning the Uri object
 */
fun getOrMakeThumbNail(thumbnail: String, image: String, context: Context): Uri?{
    var thumbnailUri: Uri? = null

    if(thumbnail.isNullOrBlank() || !File(context.cacheDir, thumbnail).exists()){

        Uri.parse(image).let {
            val thumbnailBitmap
                    = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(it, Size(250, 250), null)
            }else{
                decodeSampledBitmapFromResource(Uri.parse(image), 250, 250, context.contentResolver)
            }
            thumbnailUri = saveBitmapToCache(thumbnailBitmap, context)
        }
    }else{
        thumbnailUri = Uri.parse(thumbnail)
    }

    return thumbnailUri!!
}

fun Image.getOrMakeThumbNail(context: Context) {
    val thumbnailStringPath = this.thumbnail?.toString() ?: String()
    this.thumbnail = getOrMakeThumbNail(thumbnailStringPath, this.imagePath.toString(), context)
}

fun Image.deleteThumbnail(context: Context): Boolean {
    // Start intent and include data to let the calling activity know a deletion happened (include position payload
    val cacheFile = File(context.cacheDir, this.thumbnail.toString())
    return cacheFile.delete()
}

private fun decodeSampledBitmapFromResource(uri: Uri, reqWidth: Int, reqHeight: Int, resolver: ContentResolver): Bitmap {
    return BitmapFactory.Options().run {

        var inmemoryBitmap: Bitmap? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri))
            } else {
                resolver.openInputStream(uri)?.use{
                    BitmapFactory.decodeStream(it)
                }
            }

        var byteArraySteam = ByteArrayOutputStream()
        var byteArray = inmemoryBitmap?.let{
            it.compress(Bitmap.CompressFormat.JPEG, 0, byteArraySteam) // Quality 0 means no change
            byteArraySteam.toByteArray()
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        inJustDecodeBounds = true
        byteArray?.let {
            BitmapFactory.decodeByteArray(byteArray, 0, it.size, this) }
        // BitmapFactory.decodeFile(filePath, this) - old code before refactoring

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false
        return byteArray?.let {
            BitmapFactory.decodeByteArray(byteArray, 0, it.size, this) }!!
        // note the use of kotlin null assert !! - https://kotlinlang.org/docs/null-safety.html#the-operator
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image

    val height = options.outHeight; val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = (height / 2).toInt()
        val halfWidth = (width / 2).toInt()

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) >= reqHeight
            && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2;
        }
    }
    return inSampleSize.toInt()
}

/**
 * Saves a bitmap of the thumbnail to cache
 */
private fun saveBitmapToCache(bitmap: Bitmap, context: Context): Uri?{
    var thumbnailCacheFile: File? = null

    bitmap.let{
        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        it.compress(Bitmap.CompressFormat.JPEG, 0, bos)
        val bitmapByteArray = bos.toByteArray()

        // Save the byte array to file in the file cache directory
        // ideally you want to check cache quota before doing this.
        thumbnailCacheFile = File.createTempFile("Com6510_", ".jpg", context.cacheDir)
        val fos = FileOutputStream(thumbnailCacheFile)
        fos.write(bitmapByteArray)
        fos.flush()
        fos.close()
    }

    return thumbnailCacheFile?.toUri()
}