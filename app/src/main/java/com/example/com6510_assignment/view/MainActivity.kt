package com.example.com6510_assignment.view

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.com6510_assignment.TripAppCompatActivity
import com.example.com6510_assignment.R
import com.example.com6510_assignment.model.Image
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

// Note the use of ImageAppCompatActivity - which is a custom class that simply inherits
// the Android AppCompatActivity class and provides the ImageViewModel as a property (DRY)
class MainActivity : TripAppCompatActivity() {
    val NUMBER_OF_COLOMNS = 4
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private var adapterData: MutableList<Image>? = null

    val showImageActivityResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        result?.let{
            val position = it.data?.extras?.getInt("position") ?:  -1 // position not used, but may be useful in some cases
            val delete_op = it.data?.extras?.getBoolean("deletion")
            val update_op = it.data?.extras?.getBoolean("updated")
            delete_op?.apply {
                if(delete_op == true){
                    Snackbar.make(/* view = */ recyclerView,
                        /* text = */ "Image deleted.",
                        /* duration = */ Snackbar.LENGTH_LONG)
                        .show()
                }
            }
            update_op?.apply {
                if(update_op == true){
                    Snackbar.make(/* view = */ recyclerView,
                        /* text = */ "Image detail updated.",
                        /* duration = */ Snackbar.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check needed permissions are granted, otherwise request them

        // Set up the adapter - easier with ListAdapter and observing of the data from the ViewModel
        recyclerView = findViewById<RecyclerView>(R.id.my_list)
        adapter = ImageAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, NUMBER_OF_COLOMNS)

        // start observing the date from the ViewModel
        imageViewModel.images.observe(this) {
            // update the dataset used by the Adapter
            it?.let {
                adapter.submitList(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            //true
            val intent = Intent(this, MapsActivityCurrentPlace::class.java)
            startActivity(intent)
            return true
        } else super.onOptionsItemSelected(item)
    }

    companion object {
        const val TAG = R.string.app_name.toString()
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val REQUEST_CODE_PERMISSIONS = 10
    }

    /**
     * Unlike the previous implementation, this extends a ListAdapter (instead of a
     * RecycleView.Adapter. A ListAdapter is better suited for dynamic list (with LiveData).
     * Extending a ListAdapter also makes it easier to sort the items in the list,
     * though you will need to implement a suitable comparator.
     *
     * See: https://stackoverflow.com/questions/66485821/list-adapter-vs-recycle-view-adapter
     * https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter
     *
     * Declaring the ImageAdapter as an inter class in this implementation keeps
     * closely (and naturally) related code together. The ImageAdapter class after all is
     * a class that is part of the view. If the Adapter class is big, you may want to reconsider this.
     * In this implementation, such practice also makes it easier to handle the onClick for each of
     * the Holder items, because an inter class can access the outer class' members. This helps
     * even further to logically organize the code better.
     */
    class ImageAdapter: ListAdapter<Image, ImageAdapter.ImageViewHolder>(ImageAdapter.ImageViewHolder.ImageComparator()) {
        // Notice we no longer are passing the context in the constructor and we don't need a
        // constructor either because all that was all part of setting up the boiler
        // plate coding needed to ensure we can notify of data changes.
        lateinit var context: Context


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            context = parent.context
            return ImageViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            // All the logic in the onBindViewHolder that is handling
            // thumbnail processing are not suitable inside a view related class and have
            // been moved into the repository, making the view related classes much cleaner.
            holder.bind(getItem(position), position, context)
        }

        class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById<View>(R.id.image_item) as ImageView

            fun bind(image: Image, position: Int, context: Context){

                imageView.setImageURI(image.thumbnail)

                itemView.setOnClickListener(View.OnClickListener {
                    // the listener is implemented in MainActivity
                    // but this function delegate allows invocation
                    onViewHolderItemClick(image.id, position, context)
                })
            }

            companion object{
                internal fun create(parent: ViewGroup): ImageViewHolder {
                    //Inflate the layout, initialize the View Holder
                    val view: View = LayoutInflater.from(parent.context).inflate(
                        R.layout.list_item_image,
                        parent, false
                    )
                    return ImageViewHolder(view)
                }

                /**
                 * onClick listener for the Adapter's ImageViewHolder item click
                 * No need to have an ActivityResultContract because that was
                 * all part of the boiler plate to handle data changes
                 */
                protected fun onViewHolderItemClick(id: Int, position: Int, context: Context) {
                    val intent = Intent(context, ShowImageActivity::class.java)
                    intent.putExtra("id", id)
                    intent.putExtra("position", position)
                    (context as MainActivity).showImageActivityResultContract.launch(intent)
                }
            }

            /**
             * Comparator class used by the ListAdapter.
             */
            class ImageComparator : DiffUtil.ItemCallback<Image>() {
                override fun areItemsTheSame(oldImage: Image, newImage: Image): Boolean {
                    return oldImage.id === newImage.id
                }

                override fun areContentsTheSame(oldImage: Image, newImage: Image): Boolean {
                    return oldImage.equals(newImage)
                }
            }
        }

    }
}