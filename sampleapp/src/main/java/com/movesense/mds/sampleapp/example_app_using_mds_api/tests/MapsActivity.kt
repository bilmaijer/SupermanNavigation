package com.movesense.mds.sampleapp.example_app_using_mds_api.tests

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.CompoundButton
import android.widget.Toast
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.jjoe64.graphview.series.DataPoint
import com.movesense.mds.*
import com.movesense.mds.sampleapp.BleManager
import com.movesense.mds.sampleapp.MdsRx
import com.movesense.mds.sampleapp.R
import com.movesense.mds.sampleapp.example_app_using_mds_api.FormatHelper
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.InfoResponse
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MagneticField
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MovesenseConnectedDevices
import com.polidea.rxandroidble.RxBleDevice
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, BleManager.IBleConnectionMonitor {

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    private val MAGNETIC_FIELD_INFO_PATH = "/Meas/Magn/Info"
    private val spinnerRates = ArrayList<String>()
    private var rate: String? = null
    private val handler = Handler()
    private var mdsSubscription: MdsSubscription? = null
    val URI_EVENTLISTENER = "suunto://MDS/EventListener"
    private val MAGNETIC_FIELD_PATH = "Meas/Magn/"

    private val readingsX = ArrayList<Double>()
    private val readingsY = ArrayList<Double>()
    private val readingsZ = ArrayList<Double>()

    internal var avgX: Double? = 0.toDouble()
    internal var avgY: Double? = 0.toDouble()
    internal var avgZ: Double? = 0.toDouble()
    internal var deg: Double = 0.toDouble()
    internal var vibrator: Vibrator? = null
    internal var vibrateSpeed = 3
    internal var canVibrate = false
    internal var minY = -277.0
    internal var maxY = -229.0
    internal var minX = 37.0
    internal var maxX = 88.0
    internal var balanceZ = -70.0


    private val TAG = MapsActivity::class.java.simpleName
    private lateinit var mMap: GoogleMap
    private val PERMISSION_REQUEST_READ_LOCATION = 0
    var locationClient: FusedLocationProviderClient? = null
    private lateinit var mLocationCallback: LocationCallback
    private var destination: Location? = null
    private var current: Location? = null
    private var bearing: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    for (location in it.locations) {
                        current = location
                        destination?.let {
                            bearing = current?.bearingTo(it)
                            deg_target.text = "Target degrees: $bearing"
                        }
                    }
                }
            }
        }

        Mds.builder().build(this).get(MdsRx.SCHEME_PREFIX
                + MovesenseConnectedDevices.getConnectedDevice(0).serial + MAGNETIC_FIELD_INFO_PATH, null, object : MdsResponseListener {
            override fun onSuccess(data: String) {
                Log.d(TAG, "onSuccess(): " + data)

                val infoResponse = Gson().fromJson(data, InfoResponse::class.java)

                for (inforate in infoResponse.content.sampleRates) {
                    spinnerRates.add(inforate.toString())

                    // Set first rate as default
                    if (rate == null) {
                        rate = inforate.toString()
                    }
                }
            }

            override fun onError(error: MdsException) {
                Log.e(TAG, "onError(): ", error)
            }
        })

        BleManager.INSTANCE.addBleConnectionMonitorListener(this)
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
        Log.i("MAP", "read")
        mMap = googleMap

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        map_button.setOnClickListener {
            if (mdsSubscription == null) {
                bearing = current?.bearingTo(destination)
                Log.i(TAG, "$bearing")
                onStartListeningToSensor()
                map_button.text = "Stop"
                vibrator?.vibrate(100)
                vibrator?.cancel()
                deg_target.text = "Target degrees: $bearing"
            }
            else {
                unSubscribe()
                map_button.text = "Start"
                deg_target.text = ""
                deg_current.text = ""
            }
        }

        mMap.setOnMapClickListener({ point ->
            // TODO Auto-generated method stub
            destination = Location(LocationManager.GPS_PROVIDER)
            destination?.latitude = point.latitude
            destination?.longitude = point.longitude
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(point))
        })

        locationClient = LocationServices.getFusedLocationProviderClient(this)
        askLocationPermissions()
    }

    fun onStartListeningToSensor() {
        handler.postDelayed(runnable, 500)

        mdsSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
                FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                        .serial, MAGNETIC_FIELD_PATH + rate), object : MdsNotificationListener {
            override fun onNotification(data: String) {
                Log.d(TAG, "onSuccess(): " + data)

                val magneticField = Gson().fromJson(
                        data, MagneticField::class.java)

                if (magneticField != null) {

                    val arrayData = magneticField.body.array[0]

                    readingsX.add(arrayData.x)
                    readingsY.add(arrayData.y)
                    readingsZ.add(arrayData.z)
                }
            }

            override fun onError(error: MdsException) {
                Log.e(TAG, "onError(): ", error)
            }
        })
    }

    private val runnable = object : Runnable {
        override fun run() {
            /* do what you need to do */

            avgZ = calcAvg(readingsZ)
            avgX = calcAvg(readingsX)
            avgY = calcAvg(readingsY)

            avgX?.let {
                val x = it
                avgY?.let {
                    val y = it
                    avgZ?.let {
                        val z = it
                        deg = calcDegree(x, y)

                        readingsX.clear()
                        readingsY.clear()
                        readingsZ.clear()


                        if (z < -65) {
                            canVibrate = true
                            /* and here comes the "trick" */

                            bearing?.let {
                                Log.i(TAG, "Vals: $deg, $it")
                                deg_current.text = "Current degrees: $deg"
                                if (Math.abs(deg - it) < 9 && vibrateSpeed != 0 && canVibrate) {
                                    Log.i("MagneticVibrate", "vibrate 1")
                                    vibrateSpeed = 0
                                    val pattern = longArrayOf(0, 100, 100)
                                    vibrator?.vibrate(pattern, 0)
                                } else if (Math.abs(deg - it) < 45 && vibrateSpeed != 1 && canVibrate) {
                                    Log.i("MagneticVibrate", "vibrate 2")

                                    vibrateSpeed = 1
                                    val pattern = longArrayOf(0, 100, Math.abs(deg - 25).toLong() * 40)
                                    vibrator?.vibrate(pattern, 0)
                                } else if (canVibrate && vibrateSpeed != 2) {
                                    Log.i("MagneticVibrate", "vibrate 3")

                                    vibrateSpeed = 2
                                    val pattern = longArrayOf(0, 100, 2000)
                                    vibrator?.vibrate(pattern, 0)
                                }
                                else {

                                }
                            }

                        } else if (canVibrate) {
                            Log.i("MagneticVibrate", "vibrate cancel")

                            canVibrate = false
                            vibrator?.vibrate(100)
                            vibrator?.cancel()
                        }
                        else {
                            deg_current.text = "Not searching"

                        }
                    }
                }
            }

            handler.postDelayed(this, 500)
        }
    }

    private fun calcAvg(list: List<Double>): Double? {
        if (list.isEmpty()) {
            return null
        }
        val listSize = list.size

        val sum = list.sum()

        return sum / listSize
    }

    private fun calcDegree(x: Double, y: Double): Double {
        /*double accXnorm = accx/Math.sqrt(accx * accx + accy * accy + accz * accz);
        double accYnorm = accy/Math.sqrt(accx * accx + accy * accy + accz * accz);

        double pitch = Math.asin(accXnorm);
        double roll = -Math.asin(accYnorm / Math.cos(pitch));

        double magXcomp = magx*Math.cos(pitch)+magz*Math.sin(pitch);
        double magYcomp =magx*Math.sin(roll)*Math.sin(pitch)+magy*Math.cos(roll)-magz*Math.sin(roll)*Math.cos(pitch);

        double heading = 180 * Math.atan2(magYcomp, magXcomp);

        if (heading < 0)
            heading += 360;

        return heading;*/

        val xCentre = minX + Math.abs((maxX - minX) / 2)
        val yDiff = Math.abs(maxY - minY)
        val yOnCircle = Math.abs(minY - y)

        val value = if (x < xCentre) -yOnCircle else yOnCircle
        val deg = 90 / (yDiff / 2) * value

        Log.i(TAG, "Degree: $deg")

        return deg

    }

    private fun askLocationPermissions() {
        if (hasLocationPermission()) {
            Log.i(TAG, "has permission")
            startLocationUpdates()

            mMap.isMyLocationEnabled = true
            mMap.setOnMyLocationButtonClickListener(this)
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

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        Log.i(TAG, "result")

        when (requestCode) {
            PERMISSION_REQUEST_READ_LOCATION -> {
                Log.i(TAG, "result ok")

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                    mMap.isMyLocationEnabled = true
                    mMap.setOnMyLocationButtonClickListener(this)
                }
                return
            }
        }
    }

    override fun onDisconnect(rxBleDevice: RxBleDevice?) {
        Log.e(TAG, "onDisconnect: " + rxBleDevice?.getName() + " " + rxBleDevice?.getMacAddress())

    }

    override fun onConnect(rxBleDevice: RxBleDevice?) {
        Log.e(TAG, "onConnect: " + rxBleDevice?.getName() + " " + rxBleDevice?.getMacAddress())

    }

    override fun onPause() {
        super.onPause()
        canVibrate = false
        vibrator?.vibrate(100)
        vibrator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()

        unSubscribe()

        vibrator?.vibrate(100)
        vibrator?.cancel()
        BleManager.INSTANCE.removeBleConnectionMonitorListener(this)
    }

    private fun unSubscribe() {
        if (mdsSubscription != null) {
            mdsSubscription?.unsubscribe()
            mdsSubscription = null
        }
    }
}
