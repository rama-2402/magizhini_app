package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityMapsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class MapsActivity : BaseActivity(), OnMapReadyCallback, android.location.LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var locationManager: LocationManager
    var currentMarker: com.google.android.gms.maps.model.Marker? = null

    var counter: Int = 1

//    private var profilePicUri: Uri? = null
    private var gpsLatitude: String = ""
    private var gpsLongitude: String = ""
    private var gpsAddress: String = ""


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps)

//        profilePicUri = intent.getStringExtra(Constants.PROFILE_PIC_URI)?.toUri()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

         lifecycleScope.launch {
               delay(5000)
                if(currentMarker == null) {
                    fetchLocation()
                }
            }

        binding.locator.setOnClickListener {
            currentMarker?.remove()
            fetchLocation()
        }

        binding.submitLocation.setOnClickListener {

            counter = 1

//        val sharedPref = this.getSharedPreferences(Constants.USERS, Context.MODE_PRIVATE)
//        sharedPref.edit()
//            .apply {
//                this.putString(Constants.GPS_ADDRESS, gpsAddress)
//                this.putString(Constants.GPS_LATITUDE, gpsLatitude)
//                this.putString(Constants.GPS_LONGITUDE, gpsLongitude)
//            }.apply()

            Intent(this, ProfileActivity::class.java).also {
//                it.putExtra(Constants.PROFILE_PIC_URI, profilePicUri)
                it.putExtra(Constants.GPS_LATITUDE, gpsLatitude)
                it.putExtra(Constants.GPS_LONGITUDE, gpsLongitude)
                it.putExtra(Constants.GPS_ADDRESS, gpsAddress)
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finish()
                finishAndRemoveTask()
            }

        }
    }

    private fun fetchLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.ACCESS_LOCATION)

        } else {

            val task = fusedLocationProviderClient.lastLocation

            task.addOnSuccessListener { location: Location? ->

                if (location != null) {

                    binding.hintText.text = getString(R.string.msg_long_press_and_drag)

                    this.currentLocation = location

                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    val mapFragment = supportFragmentManager
                        .findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this)

                } else {

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1f, this)

                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.ACCESS_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                fusedLocationProviderClient =
                    LocationServices.getFusedLocationProviderClient(this)
                fetchLocation()

            } else {
                showErrorSnackBar(getString(R.string.err_location_access_denied), true)
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

            val latlong: LatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            drawMarker(latlong)

        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(p0: com.google.android.gms.maps.model.Marker?) {

            }

            override fun onMarkerDrag(p0: com.google.android.gms.maps.model.Marker?) {

            }

            override fun onMarkerDragEnd(p0: com.google.android.gms.maps.model.Marker?) {
                if (currentMarker != null)
                    currentMarker?.remove()

                gpsLatitude = p0?.position!!.latitude.toString()
                gpsLongitude = p0.position.longitude.toString()

                val newlatlng = LatLng(p0.position.latitude, p0.position.longitude)
                drawMarker(newlatlng)
            }

        })
    }

    private fun drawMarker(latlong: LatLng) {
        val markerOption = MarkerOptions().position(latlong).title(getString(R.string.msg_you_are_here))
            .snippet(getAddress(latlong.latitude, latlong.longitude)).draggable(true)

        mMap.animateCamera(CameraUpdateFactory.newLatLng(latlong))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlong, 18f))
        currentMarker = mMap.addMarker(markerOption)
        currentMarker?.showInfoWindow()

    }

    private fun getAddress(latitude: Double, longitude: Double): String {

        val geoCoder = Geocoder(this, Locale.getDefault())
        val address = geoCoder.getFromLocation(latitude, longitude, 1)

        gpsAddress = address[0].getAddressLine(0).toString()
        gpsLatitude = latitude.toString()
        gpsLongitude = longitude.toString()

        Log.e("qqqq", "$gpsAddress $gpsLatitude $gpsLongitude")

        return address[0].getAddressLine(0).toString()

    }

    override fun onBackPressed() {
        counter = 1
        finish()
        super.onBackPressed()
    }

    override fun onResume() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocation()
        super.onResume()
    }

    override fun onRestart() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocation()
        super.onRestart()
    }

    override fun onLocationChanged(location: Location) {

        currentLocation = location

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

}