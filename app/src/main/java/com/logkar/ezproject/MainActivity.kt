package com.logkar.ezproject

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.Style
import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.animation.BounceInterpolator
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {
    private lateinit var currentRoute: DirectionsRoute
    private var navigationMapRoute: NavigationMapRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_main)

        checkPermission()

        mapView.onCreate(savedInstanceState)

        val pointTarget = LatLng(-6.246860, 107.031000)
        mapView.getMapAsync {mapboxMap->
            mapboxMap.addMarker(MarkerOptions().position(pointTarget).title("Lokasi tujuan"))
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {style->

                val mLocationComponent = mapboxMap.locationComponent

                val mLocationComponentOptions = LocationComponentOptions
                    .builder(this)
                    .pulseEnabled(true)
                    .pulseColor(Color.GREEN)
                    .pulseAlpha(.4f)
                    .pulseInterpolator(BounceInterpolator())
                    .build()

                val mLocationComponentActivationOptions = LocationComponentActivationOptions
                    .builder(this, style)
                    .locationComponentOptions(mLocationComponentOptions)
                    .build()

                mLocationComponent.activateLocationComponent(mLocationComponentActivationOptions)
                mLocationComponent.isLocationComponentEnabled = true
                mLocationComponent.cameraMode = CameraMode.TRACKING_GPS
                mLocationComponent.renderMode = RenderMode.GPS

                // GET MY CURRENT LOCATION
                val pointCurrent =
                    mLocationComponent.lastKnownLocation?.latitude?.let {lat->
                        mLocationComponent.lastKnownLocation?.longitude?.let { lng ->
                            LatLng(lat, lng)
                        }
                    }
                Toast.makeText(this, "$pointCurrent", Toast.LENGTH_SHORT).show()

                val mLatLngBounds = LatLngBounds.Builder()
                    .include(pointCurrent!!)
                    .include(pointTarget)
                    .build()
                mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mLatLngBounds, 150, 300, 150, 900))

                myRoute(mapboxMap, pointCurrent, pointTarget)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun myRoute(mapboxMap: MapboxMap, origin: LatLng, destination: LatLng){
        val originPoint = Point.fromLngLat(origin.longitude, origin.latitude)
        val destinationPoint = Point.fromLngLat(destination.longitude, destination.latitude)
        val estimatedDistances = TurfMeasurement.distance(originPoint, destinationPoint)
            .toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
        tv_distance.text = "Prediksi Jarak : $estimatedDistances km"
        NavigationRoute.builder(this)
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(originPoint)
            .destination(destinationPoint)
            .voiceUnits(DirectionsCriteria.IMPERIAL)
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    if (response.body() ==  null){
                        Toast.makeText(this@MainActivity, "Gagal memperoleh rute", Toast.LENGTH_SHORT).show()
                        return
                    }
                    else if (response.body()!!.routes().size<1){
                        Toast.makeText(this@MainActivity, "Gagal memperoleh rute", Toast.LENGTH_SHORT).show()
                        return
                    }
                    currentRoute = response.body()!!.routes()[0]
                    try {
                        if (navigationMapRoute !=  null){
                            navigationMapRoute?.removeRoute()
                        }
                        else {
                            navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute)
                        }
                        navigationMapRoute?.addRoute(currentRoute)
                    }
                    catch (e: Exception){
                        Toast.makeText(this@MainActivity, "Jaringan Bermasalah ${e.cause}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Jaringan Bermasalah ${t.cause}", Toast.LENGTH_SHORT).show()
                    return
                }
            })
    }


    private fun checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
            else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}