package com.movesense.mds.sampleapp.example_app_using_mds_api.tests

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.location.*
import com.movesense.mds.sampleapp.R


class DirectionsActivity: AppCompatActivity() {
    val TAG = DirectionsActivity::class.java.simpleName
    private val PERMISSION_REQUEST_READ_LOCATION = 0
    private val LOCATION_UPDATE_REQUEST = 1
    var locationClient: FusedLocationProviderClient? = null
    private lateinit var mLocationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directions)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                Log.i(TAG, "result")
                locationResult?.let {
                    for (location in it.locations) {
                        val lat = location.latitude
                        val lng = location.longitude
                        Log.i(TAG, "$lat, $lng")
                    }
                }
            }
        }
        askLocationPermissions()
    }

    private fun askLocationPermissions() {
        if (hasLocationPermission()) {
            Log.i(TAG, "has permission")
            getLocation()
        } else {
            Log.i(TAG, "request")

            requestLocationPermission()
        }
    }

    private fun startLocationUpdates() {
        Log.i(TAG, "start updates")
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5
        locationRequest.fastestInterval = 5

        locationClient?.let {
            Log.i(TAG, "client not null")
            it.requestLocationUpdates(locationRequest,
                mLocationCallback,
                null /* Looper */)
        }
    }

    // Check if permissions already granted
    private fun hasLocationPermission(): Boolean =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Request permissions
    private fun requestLocationPermission() {
        val req = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val ctx = this
        val activity = this

        // Explain why permission needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(TAG, "explain")


            val builder = AlertDialog.Builder(ctx)
            builder.setTitle("Need permission to read location.")
            builder.setMessage("This app needs permission to show the weather in current location.")
            builder.setPositiveButton("Grant", { dialog, _ ->
                dialog.cancel()
                ActivityCompat.requestPermissions(this, req, PERMISSION_REQUEST_READ_LOCATION)
            })
            builder.setNegativeButton("Cancel", { dialog, _ -> dialog.cancel() })
            builder.show()

        } else {
            Log.i(TAG, "ask")

            // Ask permission
            ActivityCompat.requestPermissions(this, req,
                    PERMISSION_REQUEST_READ_LOCATION);
        }
    }

    private fun getLocation() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()

        locationClient?.let {
            it.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val lat = location.latitude
                        val lng = location.longitude
                        Log.i(TAG, "$lat, $lng")
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        Log.i(TAG, "result")

        when (requestCode) {
            PERMISSION_REQUEST_READ_LOCATION -> {
                Log.i(TAG, "result ok")

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation()
                }
                return
            }
        }
    }
}