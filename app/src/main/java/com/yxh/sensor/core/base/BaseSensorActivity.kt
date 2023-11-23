package com.yxh.sensor.core.base

import android.Manifest
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel

abstract class BaseSensorActivity<VM : ViewModel, VB : ViewDataBinding> : BaseSwipeLeftActivity<VM, VB>() {

    val mSensorManager: SensorManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getSystemService(SensorManager::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionRequester.launch(arrayOf(
                    Manifest.permission.BODY_SENSORS,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            permissionRequester.launch(arrayOf(
                    Manifest.permission.BODY_SENSORS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * 注册运动传感器监听器
     */
    abstract fun setSensorListener()
    /**
     * 注销运动传感器监听器
     */
    abstract fun unRegisterSensorListener()
    /**
     * 设置监听器持续监听位置信息
     */
    abstract fun setLocationListener()

    private val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            var allLocationPermissionGranted = true
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                println("成功获取ACCESS_FINE_LOCATION权限")
            } else {
                println("获取ACCESS_FINE_LOCATION权限失败")
            }

            if (it[Manifest.permission.BODY_SENSORS] == true) {
                println("成功获取BODY_SENSORS权限")
                setSensorListener() //开启运动传感器监听
            } else {
                println("获取BODY_SENSORS权限失败")
            }

            if (it[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                println("成功获取ACCESS_COARSE_LOCATION权限")
            } else {
                allLocationPermissionGranted = false
                println("获取ACCESS_COARSE_LOCATION权限失败")
            }

            if (it[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
                println("成功获取ACTIVITY_RECOGNITION权限")
            } else {
                allLocationPermissionGranted = false
                println("获取ACTIVITY_RECOGNITION权限失败")
            }

            if (allLocationPermissionGranted) {
                setLocationListener() //设置位置监听
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        unRegisterSensorListener()
    }

}