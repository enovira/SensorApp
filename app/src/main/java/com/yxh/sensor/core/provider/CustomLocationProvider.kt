package com.yxh.sensor.core.provider

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat

class CustomLocationProvider private constructor(): LocationListener {

    private var locationManager: LocationManager? = null

    private var locationCallback: ((Location) -> Unit)? = null


    companion object {
        @Volatile
        private var instance: CustomLocationProvider? = null

        @JvmStatic
        fun getInstance(): CustomLocationProvider {
            return instance ?: synchronized(this) {
                instance ?: CustomLocationProvider().also {
                    instance = it
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        locationCallback?.invoke(location)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        println("Location provider status changed: $provider, status: $status")
    }

    override fun onProviderEnabled(provider: String) {
        super.onProviderEnabled(provider)
    }

    override fun onProviderDisabled(provider: String) {
        super.onProviderDisabled(provider)
    }
    /**
     * 检查定位服务是否启用
     */
    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            val locationMode = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            locationMode != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    /**
     * 初始化定位提供者
     */
    fun startLocationListener(context: Context) {
        locationManager = context.getSystemService(LocationManager::class.java)
        // 检查定位服务是否启用
        if (!isLocationEnabled(context)) {
            Toast.makeText(context, "定位服务未开启，请开启定位服务", Toast.LENGTH_LONG).show()
            // 可选：引导用户跳转到定位设置页面
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        locationManager?.getBestProvider(createFineCriteria(), true)?.let {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationManager?.requestLocationUpdates(it, 30 * 1000, 0f, this)
        }
    }

    /** this criteria needs high accuracy, high power and cost */
    fun createFineCriteria(): Criteria {
        val c = Criteria()
        c.setAccuracy(Criteria.ACCURACY_FINE);//高精度
        c.setAltitudeRequired(true);//包含高度信息
        c.setBearingRequired(true);//包含方位信息
        c.setSpeedRequired(true);//包含速度信息
        c.setCostAllowed(true);//允许付费
        c.setPowerRequirement(Criteria.POWER_HIGH);//高耗电
        return c;
    }

    fun registerLocationCallback(context: Context ,callback: (Location) -> Unit) {
        this.locationCallback = callback
        startLocationListener(context)
    }

    fun unregisterLocationCallback() {
        this.locationCallback = null
        locationManager?.removeUpdates(this)
        locationCallback = null
    }

}