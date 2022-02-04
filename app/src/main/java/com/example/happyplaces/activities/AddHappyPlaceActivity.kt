package com.example.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.models.HappyPlaceModel
import com.example.happyplaces.utils.GetAddressFromLatLng
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity() ,View.OnClickListener {

    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude :Double = 0.0
    private var mLongitude :Double = 0.0

    private var mHappyPlaceDetails: HappyPlaceModel? = null

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient


    companion object{
        private const val GALLERY_REQUEST_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImage"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)

        setSupportActionBar(toolbar_add_place)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_add_place.setNavigationOnClickListener {
            onBackPressed()
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (!Places.isInitialized()){
            Places.initialize(this, resources.getString(R.string.google_maps_api_key))
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        dateSetListener = DatePickerDialog.OnDateSetListener {
                view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR , year)
            cal.set(Calendar.MONTH , month)
            cal.set(Calendar.DAY_OF_MONTH , dayOfMonth)
            updateDateInView()
        }

        updateDateInView()

        if (mHappyPlaceDetails != null){
            supportActionBar?.title = "Edit Happy Place"

            et_title.setText(mHappyPlaceDetails!!.title)
            et_description.setText(mHappyPlaceDetails!!.description)
            et_date.setText(mHappyPlaceDetails!!.date)
            et_location.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
            iv_place_image.setImageURI(saveImageToInternalStorage)

            btn_save.text = "UPDATE"
        }

        et_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        et_location.setOnClickListener(this)
        tv_select_current_location.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when(view?.id){

            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }

            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from gallery" , "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){
                    dialog, which ->
                    when(which){
                        0 -> {choosePhotoFromGallery()}
                        1 -> {takePhotoFromCamera()}
                    }
                }
                pictureDialog.show()
            }

            R.id.btn_save -> {
                when {
                    et_title.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }

                    et_description.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
                    }

                    et_location.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show()
                    }

                    (saveImageToInternalStorage == null) -> {
                        Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                    }

                    else -> {
                        val happyPlace = HappyPlaceModel(
                            if(mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            et_title.text.toString(),
                            saveImageToInternalStorage.toString(),
                            et_description.text.toString(),
                            et_date.text.toString(),
                            et_location.text.toString(),
                            mLatitude,
                            mLongitude
                        )

                        val dbHandler = DatabaseHandler(this)

                        if (mHappyPlaceDetails == null){
                            val addHappyPlaceResult = dbHandler.addHappyPlace(happyPlace)

                            if (addHappyPlaceResult > 0) {
                            setResult(Activity.RESULT_OK)
                            finish()
                            }
                        }
                        else{
                            val updateHappyPlace = dbHandler.updateHappyPlace(mHappyPlaceDetails!!)

                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }

            R.id.et_location -> {
                try {
                    val field = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, field).build(this)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                }
                catch (e:Exception){
                    e.printStackTrace()
                }
            }

            R.id.tv_select_current_location -> {
                if (!locationIsEnabled()){
                    Toast.makeText(this, "Your location provider turned off. please turn it on.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
                else{
                    Dexter.withContext(this)
                        .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION ).withListener(object: MultiplePermissionsListener {
                            override fun onPermissionsChecked(report : MultiplePermissionsReport?)
                            {
                                if (report!!.areAllPermissionsGranted()){
                                    requestNewLocationData()
                                }
                            }
                            override fun onPermissionRationaleShouldBeShown(permissions : MutableList<PermissionRequest>, token : PermissionToken)
                            {
                                showRationalDialogForPermission()
                            }
                        }).onSameThread().check();
                }
            }
        }
    }

    private fun takePhotoFromCamera(){
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report : MultiplePermissionsReport?)
                {
                    if (report!!.areAllPermissionsGranted()){
                        Toast.makeText(this@AddHappyPlaceActivity,"Location Permissions are Granted" +
                                ",Now you can get current location",Toast.LENGTH_SHORT).show()
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
                    }
                }
                override fun onPermissionRationaleShouldBeShown(permissions : MutableList<PermissionRequest>, token : PermissionToken)
                {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check();
    }

    private fun choosePhotoFromGallery(){
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report : MultiplePermissionsReport?)
                {
                    if (report!!.areAllPermissionsGranted()){
                        Toast.makeText(this@AddHappyPlaceActivity,"Storage Read/Write Permissions are Granted" +
                                ",Now you can select photo from gallery",Toast.LENGTH_SHORT).show()
                        val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
                    }
                }
                override fun onPermissionRationaleShouldBeShown(permissions : MutableList<PermissionRequest>, token : PermissionToken)
                {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check();
    }

    private fun showRationalDialogForPermission(){
        AlertDialog.Builder(this).setMessage("It looks like you have turned off permission required for this feature." +
                " It can be enabled under the Applications Settings").setPositiveButton("GO TO SETTINGS"){
                dialog, which ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("Package",packageName,null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException){
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel"){
                dialog, which -> dialog.dismiss()
        }.show()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY_REQUEST_CODE){
                if (data != null){
                    val contentURI=data.data
                    try {
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)
                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("Saved Image: ", "path:: $saveImageToInternalStorage ")
                        iv_place_image.setImageBitmap(selectedImageBitmap)
                    }
                    catch (e: IOException){
                        e.printStackTrace()
                        Toast.makeText(this,"Failed to load image from Gallery!",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else if (requestCode == CAMERA_REQUEST_CODE ){
                val thumbNail = data!!.extras!!.get("data") as Bitmap
                saveImageToInternalStorage = saveImageToInternalStorage(thumbNail)
                Log.e("Saved Image: ", "path:: $saveImageToInternalStorage ")
                iv_place_image.setImageBitmap(thumbNail)
            }
            else if ( requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
                val place = Autocomplete.getPlaceFromIntent(data!!)
                et_location.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        }
    }

    private fun updateDateInView(){
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        try {
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }
        catch (e : IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    private fun locationIsEnabled():Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1

        Looper.myLooper()?.let {
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack,
                it
            )
        }
    }

    private val mLocationCallBack = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            mLongitude = mLastLocation.longitude

            val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity, mLatitude, mLongitude)
            addressTask.setAddressListener(object: GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                    et_location.setText(address)
                }

                override fun onError() {
                    Log.e("Get Address:: ", "Something went wrong " )
                }
            })
            addressTask.getAddress()
        }
    }
}