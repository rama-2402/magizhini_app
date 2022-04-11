package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.contextaware.withContextAvailable
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityMapsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.PermissionsUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadInAnimation
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadOutAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*


class MapsActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    private lateinit var binding: ActivityMapsBinding

    private lateinit var mMap: GoogleMap

    private var currentLocation: Location? = null
    private var currentMarker: Marker? = null

    private var locationRequest: LocationRequest? = null
    private val locationRequestCallback = object : LocationCallback() {
        override fun onLocationResult(results: LocationResult) {
            if (results.equals(null)) {
                return
            } else {
                for (result in results.locations) {
                    if (result != null) {
                        currentLocation = result
                        Toast.makeText(
                            this@MapsActivity,
                            currentLocation!!.latitude.toString() + " " + currentLocation!!.longitude,
                            Toast.LENGTH_SHORT
                        ).show()
                        val supportMapFragment =
                            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
                        supportMapFragment.getMapAsync(this@MapsActivity)
                        break
                    }
                }
                stopLocationUpdate()
                return
            }
        }
    }
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private var markerLatitude: String = ""
    private var markerLongitude: String = ""
    private var markerAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps)

        if (PermissionsUtil.isGpsEnabled(this)) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fetchLocation()
        } else {
            showToast(this, "Turn on GPS Location from settings to access map")
            finish()
        }

        binding.locator.setOnClickListener {
            currentLocation?.let {
                currentMarker?.remove()
                currentMarker = null
                currentLocation = null
                fetchLocation()
            }
        }

        binding.submitText.setOnClickListener {
            onBackPressed()
        }

        lifecycleScope.launch {
            showToast(this@MapsActivity, "Long press to place marker in your location", LONG)
            withContext(Dispatchers.IO) {
                while (true) {
                    delay(5000)
                    currentMarker?.let {
                        if (binding.hint.isVisible) {
                            withContext(Dispatchers.Main) {
                                binding.hint.fadOutAnimation()
                                binding.hint.remove()
                            }
                        }
                    } ?: let {
                        withContext(Dispatchers.Main) {
                            if (!binding.hint.isVisible) {
                                binding.hint.fadInAnimation()
                                binding.hint.visible()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        val task = fusedLocationProviderClient!!.lastLocation

        task.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                Toast.makeText(
                    this,
                    currentLocation!!.latitude.toString() + " " + currentLocation!!.longitude,
                    Toast.LENGTH_SHORT
                ).show()
                val supportMapFragment =
                    (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
                supportMapFragment.getMapAsync(this@MapsActivity)
            } else {
                locationRequest = LocationRequest.create()
                locationRequest?.interval = 10000
                locationRequest?.fastestInterval = 5000
                locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                checkSettingsAndStartLocationUpdate()
            }
        }
    }

    private fun checkSettingsAndStartLocationUpdate() {
        val locationSettingsRequest: LocationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!).build()
        val client: SettingsClient = LocationServices.getSettingsClient(this)

        val task: Task<LocationSettingsResponse> =
            client.checkLocationSettings(locationSettingsRequest)

        task.addOnSuccessListener { response ->
            startLocationUpdate()
        }

        task.addOnFailureListener { error ->
            if (error is ResolvableApiException) {
                try {
                    error.startResolutionForResult(this, 5)
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun startLocationUpdate() {
        locationRequest?.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            fusedLocationProviderClient?.requestLocationUpdates(
                it,
                locationRequestCallback,
                mainLooper
            )
        }
    }

    private fun stopLocationUpdate() {
        fusedLocationProviderClient?.removeLocationUpdates(locationRequestCallback)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mMap.isMyLocationEnabled = true

        val latLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
        moveMarket(latLng)

        mMap.setOnMapLongClickListener(this)

    }

    override fun onMapLongClick(p0: LatLng) {
        moveMarket(p0)
    }

    private fun moveMarket(latLng: LatLng) {
        currentMarker?.remove()
        val address = getTheAddress(latLng.latitude, latLng.longitude)
        val markerOptions = MarkerOptions().position(latLng).title("I am here")
            .snippet(address).draggable(true)
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        currentMarker = mMap.addMarker(markerOptions)
        currentMarker?.showInfoWindow()
    }

    private fun getTheAddress(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            markerLatitude = latitude.toString()
            markerLongitude = longitude.toString()
            markerAddress = addresses[0].getAddressLine(0)
            markerAddress
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdate()
    }

    override fun onBackPressed() {
        val intent = Intent()
        intent.putExtra("latitude", markerLatitude)
        intent.putExtra("longitude", markerLongitude)
        intent.putExtra("gpsAddress", markerAddress)
        setResult(123, intent)
        finish()
    }
}
