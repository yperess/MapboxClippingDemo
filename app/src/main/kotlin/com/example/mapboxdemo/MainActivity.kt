package com.example.mapboxdemo

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions
import com.mapbox.mapboxsdk.constants.MyLocationTracking
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationSource
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.telemetry.location.LocationEngine
import com.mapbox.services.android.telemetry.location.LocationEngineListener
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

fun Context.iconFactory(): IconFactory = IconFactory.getInstance(this)

class MainActivity : AppCompatActivity(), LocationEngineListener {

    companion object {
        private val TAG = "MainActivity"
        private val IMAGE_URLS = arrayOf(
                "https://metaverse.imgix.net/ea654773-6968-4266-8930-8fe697752ad7.png?w=110&h=110",
                "https://metaverse.imgix.net/f0535170-ef0a-441f-be76-2c9c1d2ebfd2.png?w=110&h=110"
        )
        private const val DIV = 700
        private const val CENTER = .5f / DIV
    }

    private val random = Random()
    private var mapboxMap: MapboxMap? = null
    private lateinit var locationEngine: LocationEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)

        Picasso.with(this).apply {
            isLoggingEnabled = true
            setIndicatorsEnabled(true)
        }

        locationEngine = LocationSource.getLocationEngine(this)
        map.onCreate(savedInstanceState)
        map.getMapAsync {
            Log.d(TAG, "Map is ready")
            mapboxMap = it.apply {
                trackingSettings.isDismissLocationTrackingOnGesture = false
                trackingSettings.myLocationTrackingMode = MyLocationTracking.TRACKING_FOLLOW
                setMinZoomPreference(16.0)
                setMaxZoomPreference(21.0)
                setOnMyLocationChangeListener { onLocationChanged(it) }
                onLocationChanged(locationEngine.lastLocation)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        map.onStart()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 1000);
        }
        locationEngine.activate()
        locationEngine.addLocationEngineListener(this)
        locationEngine.requestLocationUpdates()
        onLocationChanged(locationEngine.lastLocation)
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        locationEngine.deactivate()
        locationEngine.removeLocationUpdates()
        locationEngine.removeLocationEngineListener(this)
    }

    override fun onStop() {
        super.onStop()
        map.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map.onLowMemory()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
            grantResults: IntArray) {
        when (requestCode) {
            1000 -> {
                if (grantResults[0] != PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permissions are required",
                            Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //
    // LocationEngineListener
    //

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            return
        }
        Log.d(TAG, "onLocationChanged ${location.toString()}, mapboxMap = $mapboxMap")
        mapboxMap?.let {
            it.clear()
            for (i in 0..9) {
                Picasso.with(this)
                        .load(IMAGE_URLS[random.nextInt(IMAGE_URLS.size)])
                        .into(object: Target {
                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                            }

                            override fun onBitmapFailed(errorDrawable: Drawable?) {
                            }

                            override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                                showBitmap(bitmap)
                            }
                        })
            }
        }
    }

    override fun onConnected() {
        Log.d(TAG, "Location engine connected")
        onLocationChanged(locationEngine.lastLocation)
    }

    fun showBitmap(bitmap: Bitmap) {
        val latLng = LatLng(locationEngine.lastLocation).apply {
            latitude = latitude + random.nextDouble() / DIV - CENTER
            longitude = longitude + random.nextDouble() / DIV - CENTER
        }
        Log.d(TAG, "Adding marker @ ${latLng}")
        mapboxMap?.addMarker(MarkerViewOptions()
                .flat(false)
                .position(latLng)
                .icon(iconFactory().fromBitmap(bitmap)))
    }
}
