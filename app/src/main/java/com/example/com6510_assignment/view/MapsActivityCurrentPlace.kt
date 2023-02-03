// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.example.com6510_assignment.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewbinding.BuildConfig
import com.example.com6510_assignment.R
import com.example.com6510_assignment.TripAppCompatActivity
import com.example.com6510_assignment.view.MapsActivityCurrentPlace.Companion.MAPS_API_KEY
import com.example.currentplacedetailsonmap.AsyncResponse
import com.example.currentplacedetailsonmap.DirectionsJSONParser
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


/**
 * An activity that displays a map showing the place at the device's current location.
 */
class MapsActivityCurrentPlace : TripAppCompatActivity(), OnMapReadyCallback {
    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null
    private var likelyPlaceNames: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAddresses: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAttributions: Array<List<*>?> = arrayOfNulls(0)
    private var likelyPlaceLatLngs: Array<LatLng?> = arrayOfNulls(0)
    private var this_trip_title: String = ""

    val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        it?.let{ uri ->
            // https://developer.android.com/training/data-storage/shared/photopicker#persist-media-file-access
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            this@MapsActivityCurrentPlace.contentResolver.takePersistableUriPermission(uri, flag)

            imageViewModel.insert(
                image_uri = uri)
        }
    }

    val pickFromCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val photo_uri = result.data?.extras?.getString("uri")

        photo_uri?.let{
            val uri = Uri.parse(photo_uri)

            imageViewModel.insert(
                image_uri = uri)
        }
    }

    // [START maps_current_place_on_create]
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, MainActivity.REQUEST_CODE_PERMISSIONS
            )
        }

        // [START_EXCLUDE silent]
        // Retrieve location and camera position from saved instance state.
        // [START maps_current_place_on_create_save_instance_state]
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }
        // [END maps_current_place_on_create_save_instance_state]
        // [END_EXCLUDE]

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps)

        // [START_EXCLUDE silent]
        // Construct a PlacesClient
        Places.initialize(applicationContext, MAPS_API_KEY)
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Build the map.
        // [START maps_current_place_map_fragment]
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        // [END maps_current_place_map_fragment]
        // [END_EXCLUDE]


        // Setup a photo picker Activity to be started when the openGalleryFab button is clicked
        // The ActivityResultContract, photoPicker, will handle the result when the photo picker Activity returns
        val photoPickerFab: FloatingActionButton = findViewById<FloatingActionButton>(R.id.openGalleryFab)
        photoPickerFab.setOnClickListener(View.OnClickListener { view ->
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        })

        // Setup the CameraActivity to be started when the openCamFab button is clicked
        // The ActivityResultContract, pickFromCamera, will handle the result when the CameraActivity returns
        val cameraPickerFab: FloatingActionButton = findViewById<FloatingActionButton>(R.id.openCamFab)
        cameraPickerFab.setOnClickListener(View.OnClickListener { view ->
            val intent = Intent(this, CameraActivity::class.java)
            pickFromCamera.launch(intent)
        })

        val openGalleryFab: FloatingActionButton = findViewById(R.id.showGalleryFab)
        openGalleryFab.setOnClickListener({ view ->
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent);
        })
    }
    // [END maps_current_place_on_create]

    private fun onSearchCalled() {
        // Set the fields to specify which types of place data to return.
        val fields: List<Place.Field> = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        // Start the autocomplete intent.
        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.FULLSCREEN, fields
        ).setCountry("US") //US
            .build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        Log.i(TAG, "Place: ${place.name}, ${place.id}")
                        showSearchedLocationDetailsDialog(place)
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    // TODO: Handle the error.
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.i(TAG, status.statusMessage ?: "")
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // The user canceled the operation.
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showSearchedLocationDetailsDialog(place: Place) {

        if(lastKnownLocation != null){
            val currLatLng = LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude)
            val destinationLatLng = place.latLng!!
            map?.moveCamera(CameraUpdateFactory.newLatLng(destinationLatLng))
            map?.addMarker(MarkerOptions().position(destinationLatLng))

            val dialog = BottomSheetDialog(this)
            val view: View = layoutInflater.inflate(R.layout.location_info_dialog, null)
            dialog.setContentView(view)
            val lp: WindowManager.LayoutParams = dialog.window!!.attributes
            lp.dimAmount = 0f
            dialog.show()

            dialog.setOnCancelListener {
                val show_dialog_btn = FloatingActionButton(this)
                val btn_param: WindowManager.LayoutParams = window.attributes
                btn_param.width = 50
                btn_param.height = 50
                btn_param.gravity = Gravity.RIGHT
                btn_param.gravity = Gravity.BOTTOM
                btn_param.title = "show"

                addContentView(show_dialog_btn, btn_param)
                show_dialog_btn.setOnClickListener {
                    showSearchedLocationDetailsDialog(place)
                }
            }

            val locationName = dialog.findViewById<TextView>(R.id.location_info_name)
            val locationLocation = dialog.findViewById<TextView>(R.id.location_info_location)
            if (locationName != null && locationLocation != null) {
                locationName.text = place.name
                locationLocation.text = place.address
            }
            dialog.findViewById<Button>(R.id.location_info_direction_btn)?.setOnClickListener {
                dialog.dismiss()
                setTripTitleDialog(place)
            }
        }else{
            getDeviceLocation()
            showSearchedLocationDetailsDialog(place)
        }

    }
    private fun setTripTitleDialog(place: Place){
        val set_title_dialog = Dialog(this)
        val set_title_view: View = layoutInflater.inflate(R.layout.set_trip_title_dialog, null)
        set_title_dialog.setContentView(set_title_view)
        set_title_dialog.show()
        set_title_dialog.findViewById<Button>(R.id.set_title_btn)?.setOnClickListener{
            this_trip_title = set_title_dialog.findViewById<EditText>(R.id.title).text.toString()
            println(this_trip_title)
            if(this_trip_title == ""){
                val textTips: AlertDialog = AlertDialog.Builder(this)
                    .setTitle("Alert:")
                    .setMessage("Title cannot be empty")
                    .create();
                textTips.show();
            }else{
                set_title_dialog.dismiss()
                Toast.makeText(this, "Loading！", Toast.LENGTH_SHORT).show();
                showModeSelectionDialog(place)
            }
        }
    }
    private fun showModeSelectionDialog(place: Place){

        val mode_select_dialog = BottomSheetDialog(this)
        val mode_select_view: View = layoutInflater.inflate(R.layout.mode_select_dialog, null)
        mode_select_dialog.setContentView(mode_select_view)

        mode_select_dialog.setOnCancelListener {
            val show_dialog_btn = FloatingActionButton(this)
            val btn_param: WindowManager.LayoutParams = window.attributes
            btn_param.width = 50
            btn_param.height = 50
            btn_param.gravity = Gravity.RIGHT
            btn_param.gravity = Gravity.BOTTOM
            btn_param.title = "show"

            addContentView(show_dialog_btn, btn_param)
            show_dialog_btn.setOnClickListener {
                showModeSelectionDialog(place)
            }
        }
        val lp: WindowManager.LayoutParams = mode_select_dialog.window!!.attributes
        lp.dimAmount = 0f

        mode_select_dialog.show()
        var mode: String? = null
        val radioGroup = mode_select_dialog.findViewById<RadioGroup>(R.id.mode_selection_radiobox)
        val radioBtn = mode_select_dialog.findViewById<RadioButton>(radioGroup!!.checkedRadioButtonId)
        mode = radioBtn?.text.toString()
        getDirections(place, mode)
        radioGroup?.setOnCheckedChangeListener { group, checkedId ->
            val radio: RadioButton = group.findViewById(checkedId)
            mode = radio.text.toString()
            getDirections(place, mode!!)
            Log.e("selectedtext-->", radio.text.toString())
        }
        mode_select_dialog.findViewById<Button>(R.id.trip_stop_btn)?.setOnClickListener {
            mode_select_dialog.dismiss()
            // store this title

            // initialize title
            this_trip_title = ""
        }

    }


    private fun getDirections(place: Place, mode: String){
        val currLatLng = LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude)
        val destinationLatLng = place.latLng!!
        var distance: String? = null
        var duration: String? = null
        val urlStr = getDirectionsRequestUrl(place, mode)
        val getDirectionsRequest = GetDirectionsRequest()
        getDirectionsRequest.execute(urlStr)
        getDirectionsRequest.setOnAsyncResponse(object : AsyncResponse {
            //通过自定义的接口回调获取AsyncTask中onPostExecute返回的结果变量
            override fun onDataReceivedSuccess(listData: PolylineOptions?, data: String?){
                if (listData != null) {
                    println(data)
                    val bounds = getDirectionsBounds(currLatLng, destinationLatLng)
                    val dataJson = JSONObject(data)
                    val parser = DirectionsJSONParser()
                    val legs = parser.parseLegs(dataJson)
                    /** Traversing all legs  */
                    for (j in 0 until (legs?.length()!!)) {
                        distance = (legs?.get(j) as JSONObject).getJSONObject("distance").getString("text")
                            .toString()
                        duration = (legs?.get(j) as JSONObject).getJSONObject("duration").getString("text")
                            .toString()
                    }
                    val directionsInfo = map?.addMarker(MarkerOptions()
                        .position(listData.points[listData.points.size/2])
                        .title("Directions Info")
                        .snippet("Distance: $distance \nDuration: $duration"))
                    directionsInfo?.showInfoWindow()
                    map?.addPolyline(listData)
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, 5f))
                }
            }
            override fun onDataReceivedFailed() {
                TODO("Not yet implemented")
            }
        })
    }


    private fun getDirectionsRequestUrl(currPlace: Place, mode: String): String{
        val destinationLat = currPlace.latLng!!.latitude
        val destinationLng = currPlace.latLng!!.longitude
        val currLat = lastKnownLocation!!.latitude
        val currLng = lastKnownLocation!!.longitude
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${currLat},${currLng}" +
                    "&destination=${destinationLat},${destinationLng}&key=$MAPS_API_KEY&mode=${mode}"

    }

    private fun getDirectionsBounds(currLagLng: LatLng, destinationLatLng: LatLng): LatLngBounds{
        val currLat = currLagLng.latitude
        val currLng = currLagLng.longitude
        val destinationLat = destinationLatLng.latitude
        val destinationLng = destinationLatLng.longitude
        var wBound = 0.0
        var eBound = 0.0
        var nBound = 0.0
        var sBound = 0.0
        if(currLng < destinationLng){
            wBound = currLng
            eBound = destinationLng
        }else{
            wBound = destinationLng
            eBound = currLng
        }
        if(currLat < destinationLat){
            nBound = destinationLat
            sBound = currLat
        }else{
            nBound = currLat
            sBound = destinationLat
        }
        val swBounds = LatLng(sBound, wBound)
        val neBound = LatLng(nBound, eBound)
        return LatLngBounds(swBounds, neBound)
    }
    // Fetches data from url passed(directions)
    private class GetDirectionsRequest : AsyncTask<String?, Void?, String?>() {
        var lineOptions: PolylineOptions? = null
        var asyncResponse: AsyncResponse? = null
        fun setOnAsyncResponse(asyncResponse: AsyncResponse?) {
            this.asyncResponse = asyncResponse
        }
        // Downloading data in non-ui thread
        @SuppressLint("LongLogTag")
        override fun doInBackground(vararg params: String?): String? {

            // For storing data from web service
            var data = ""
            try {
                // Fetching the data from web service
                var iStream: InputStream? = null
                var urlConnection: HttpURLConnection? = null
                try {
                    val url = URL(params[0])
                    // Creating an http connection to communicate with url
                    urlConnection = url.openConnection() as HttpURLConnection

                    // Connecting to url
                    urlConnection.connect()

                    // Reading data from url
                    iStream = urlConnection!!.inputStream
                    val br = BufferedReader(
                        InputStreamReader(
                            iStream
                        )
                    )
                    val sb = StringBuffer()
                    var line: String? = ""
                    while (br.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    data = sb.toString()
                    br.close()
                } catch (e: Exception) {
                    Log.d("Exception while downloading url", e.toString())
                } finally {
                    iStream!!.close()
                    urlConnection!!.disconnect()
                }
                println("url:$params[0]---->   downloadurl:$data")
            } catch (e: java.lang.Exception) {
                Log.d("Background Task", e.toString())
            }
            return data
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val jObject: JSONObject
            var routes: List<List<HashMap<String, String>>>? = null
            var points: ArrayList<LatLng?>? = null
            try {
                jObject = JSONObject(result)
                val parser = DirectionsJSONParser()

                // Starts parsing data
                routes = parser.parseRoutes(jObject)
                println("do in background:$routes")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            // Traversing through all the routes
            println("parse task success $routes")
            for (i in routes!!.indices) {
                points = ArrayList()
                lineOptions = PolylineOptions()

                // Fetching i-th route
                val path = routes[i]

                // Fetching all the points in i-th route
                for (j in path.indices) {
                    val point = path[j]
                    val lat = point["lat"]!!.toDouble()
                    val lng = point["lng"]!!.toDouble()
                    val position = LatLng(lat, lng)
                    points.add(position)
                }

                // Adding all the points in the route to LineOptions
                lineOptions!!.addAll(points)
                lineOptions!!.width(10f)
                // Changing the color polyline according to the mode
                lineOptions!!.color(Color.BLUE)
            }
            // Drawing polyline in the Google Map for the i-th route
            asyncResponse?.onDataReceivedSuccess(lineOptions, result)
        }
    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    // [START maps_current_place_on_save_instance_state]
    override fun onSaveInstanceState(outState: Bundle) {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }
    // [END maps_current_place_on_save_instance_state]
    /**
     * Sets up the options menu.
     * @param menu The options menu.
     * @return Boolean.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.map_search_menu, menu)
        return true
    }

    /**
     * Handles a click on the menu option to get a place.
     * @param item The menu item to handle.
     * @return Boolean.
     */
    // [START maps_current_place_on_options_item_selected]
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.map_search) {
            onSearchCalled()
        }
        return true
    }
    // [END maps_current_place_on_options_item_selected]
    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    // [START maps_current_place_on_map_ready]
    override fun onMapReady(map: GoogleMap) {
        this.map = map
        // [START_EXCLUDE]
        // [START map_current_place_set_info_window_adapter]
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.map?.setInfoWindowAdapter(object : InfoWindowAdapter {
            // Return null here, so that getInfoContents() is called next.
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the layouts for the info window, title and snippet.
                val infoWindow = layoutInflater.inflate(R.layout.custom_info_contents,
                    findViewById<FrameLayout>(R.id.map), false)
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker.title
                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
                snippet.text = marker.snippet
                return infoWindow
            }
        })
        // [END map_current_place_set_info_window_adapter]

        // Prompt the user for permission.
        getLocationPermission()
        // [END_EXCLUDE]

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()
    }
    // [END maps_current_place_on_map_ready]

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    // [START maps_current_place_get_device_location]
    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                            map?.addMarker(MarkerOptions().position(
                                LatLng(lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude)
                            ))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }
    // [END maps_current_place_get_device_location]

    /**
     * Prompts the user for permission to use the device location.
     */
    // [START maps_current_place_location_permission]
    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MainActivity.REQUEST_CODE_PERMISSIONS
            )
        }
    }
    // [END maps_current_place_location_permission]

    /**
     * Handles the result of the request for location permissions.
     */
    // [START maps_current_place_on_request_permissions_result]
//    override fun onRequestPermissionsResult(requestCode: Int,
//                                            permissions: Array<String>,
//                                            grantResults: IntArray) {
//        locationPermissionGranted = false
//        when (requestCode) {
//            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
//
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.isNotEmpty() &&
//                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    locationPermissionGranted = true
//                }
//            }
//            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        }
//        updateLocationUI()
//    }
    // [END maps_current_place_on_request_permissions_result]
    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    // [START maps_current_place_update_location_ui]
    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }
    // [END maps_current_place_update_location_ui]

    // Called in onCreate to check if permissions have been granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // called to request permissions
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        locationPermissionGranted = false
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MainActivity.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                locationPermissionGranted = true
                Toast.makeText(this,
                    "All permissions granted by the user.",
                    Toast.LENGTH_SHORT).show()
                updateLocationUI()
            } else {
                Toast.makeText(this,
                    "Not all permissions granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private val TAG = MapsActivityCurrentPlace::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        // [START maps_current_place_state_keys]
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
        // [END maps_current_place_state_keys]

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5

        private const val AUTOCOMPLETE_REQUEST_CODE = 1

        private const val MAPS_API_KEY="AIzaSyCmz4YH-OjV7BIfKpLWFLIo6H-6sFSz5BY"

        val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                }

            }.toTypedArray()

    }
}

