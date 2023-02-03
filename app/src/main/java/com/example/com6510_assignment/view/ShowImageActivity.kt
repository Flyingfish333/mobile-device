package com.example.com6510_assignment.view

import android.content.ContentUris
import android.content.Intent
import android.icu.text.Transliterator.Position
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.text.isDigitsOnly
import com.example.com6510_assignment.TripAppCompatActivity
import com.example.com6510_assignment.databinding.ActivityShowImageBinding
import com.example.com6510_assignment.model.Image
import com.example.com6510_assignment.utils.deleteThumbnail
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_show_image.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ShowImageActivity  : TripAppCompatActivity() {
    private lateinit var binding: ActivityShowImageBinding

    // This class didn't change so much as the other classes serve to show the examples intended well enough
    // Still, you should pay attention to the relevant changes.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // intent is a property of the activity. intent.extras returns any data that was pass
        // along with the intent.
        val bundle: Bundle? = intent.extras

        if (bundle!= null) {
            val imageId = bundle.getInt("id")
            val position = bundle.getInt("position")

            if (imageId > 0) {
                // Observe the image data, only one will be received
                imageViewModel.getImage(imageId).observe(this){
                    val image = it
                    // Display the model's data in the view. This is a lot of back and forth!
                    loadImageView(image.imagePath.toString())

                    binding.editTextTitle.setText(image.title)
                    image.description?.isNotEmpty().apply {
                        binding.editTextDescription.setText(image.description)
                    }

                    // onClick listener for the update button
                    binding.buttonSave.setOnClickListener {
                        onUpdateButtonClickListener(it, image, position)
                    }

                    // onClick listener for the delete button
                    binding.buttonDelete.setOnClickListener {
                        onDeleteButtonClickListener(it, image, position)
                    }
                }
            }
        }
    }

    /**
     * This function will either use the file path to load the image by default
     * Or can load from MediaStore when defaultToPath is false and the
     * path host contains "com.android".
     */
    private fun loadImageView(image_path: String, defaultToPath: Boolean = true){
        if(defaultToPath){
            loadImageViewWithPath(image_path)
        }else{
            val uri = Uri.parse(image_path)
            val host = uri.host ?: "media"
            val id = uri.lastPathSegment?.split(":")?.get(1) ?: ""

            if(host.startsWith("com.android")){
                runBlocking {
                    loadImageViewWithMediaStore(id)
                }
            }else{
                loadImageViewWithPath(image_path)
            }
        }
    }

    /**
     * function that loads images based on the image's file path only
     */
    private fun loadImageViewWithPath(path: String){
        binding.image.setImageURI(Uri.parse(path))
    }

    /**
     * function that loads images from media store. Queries for the image
     * using the id. Note, image may no longer exists in storage, or
     * might have been backed up to Google Photos, which might take a while to
     * retrieve. Retrieval will fail if there is no Internet connection.
     *
     * This is a basic media store access implementation. Media store query works
     * similar to running an SQL query
     */
    private suspend fun loadImageViewWithMediaStore(id: String){
        if(id.isEmpty() or !id.isDigitsOnly()){
            Snackbar.make(binding.image, "Unable to load image. Image not found.", Snackbar.LENGTH_LONG)
                .show()
            return
        }

        var current_access_uri: Uri? = null
        // Specify the columns that should be returned in the media store query result
        val projection = arrayOf(MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
        )

        withContext(Dispatchers.IO){
            contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Images.Media._ID + " = $id",
                null,
                null)?.use {cursor ->
                if(cursor.moveToFirst()){
                    // Gets a current URI for the media store item
                    current_access_uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong())
                }
            }
        }
        current_access_uri?.let{
            binding.image.setImageURI(current_access_uri)
        }
    }

    private fun onUpdateButtonClickListener(view: View, image: Image, position: Int){
        // Update the data in the model back. This is a lot of work, back and forth!
        image.title = binding.editTextTitle.text.toString()
        image.description = binding.editTextDescription.text.toString()

        imageViewModel.update(image)

        // Start an intent to let the calling activity know an update has happened.
        val intent = Intent(this@ShowImageActivity, MainActivity::class.java)
        intent.putExtra("position", position)
        intent.putExtra("updated",true)
        setResult(RESULT_OK,intent)
        finish()
    }

    private fun onDeleteButtonClickListener(view: View, image: Image, position: Int){
        image.deleteThumbnail(this@ShowImageActivity)
        imageViewModel.delete(image)

        // Start an intent to let the calling activity know a delete has happened.
        val intent = Intent(this@ShowImageActivity, MainActivity::class.java)
        intent.putExtra("position", position)
        intent.putExtra("deletion",true)
        setResult(RESULT_OK,intent)
        finish()
    }
}