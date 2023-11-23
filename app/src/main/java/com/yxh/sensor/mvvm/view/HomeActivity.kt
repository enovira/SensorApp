package com.yxh.sensor.mvvm.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.media.MediaDrm
import android.os.Build
import android.telephony.TelephonyManager
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import com.yxh.sensor.App
import com.yxh.sensor.R
import com.yxh.sensor.core.base.BaseSwipeLeftActivity
import com.yxh.sensor.core.base.PreventFastClickListener
import com.yxh.sensor.core.global.SPKey
import com.yxh.sensor.core.receiver.BatteryBroadcastReceiver
import com.yxh.sensor.core.receiver.TimeBroadcastReceiver
import com.yxh.sensor.core.utils.SPUtils
import com.yxh.sensor.databinding.ActivityHomeBinding
import com.yxh.sensor.mvvm.viewmodel.HomeActivityViewModel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Calendar
import java.util.UUID


class HomeActivity : BaseSwipeLeftActivity<HomeActivityViewModel, ActivityHomeBinding>() {

    private val batteryBroadcastReceiver by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { BatteryBroadcastReceiver() }
    private val timeBroadcastReceiver by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { TimeBroadcastReceiver() }
    private val calendar by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Calendar.getInstance() }
    private val mSensorManager by lazy { getSystemService(SensorManager::class.java) }
    private val timeStringBuilder = StringBuilder()
    private var gpsLevel = 0

    override fun getLayoutId(): Int {
        return R.layout.activity_home
    }

    override fun initView() {
        mBinding.vm = mViewModel
        checkPermissions()
        initListener()
        initObserver()
        initImei()
        initSharedPreferenceValue()
        startGifAnimation()
//            initGPSStatus()
//        println(getUniqueId())
    }

    override fun onResume() {
        super.onResume()
        mBinding.root.x = 0f
        mViewModel.viewModelScope.launch {
            updateTime()
            registerBroadcastReceiver()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterBroadcastReceiver()
        unregisterHeartRateListener()
    }

    private fun registerHeartRateListener() {
        mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)?.let {
            mSensorManager.registerListener(
                heartRateSensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterHeartRateListener() {
        mSensorManager.unregisterListener(heartRateSensorListener)
    }

    private val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                println("成功获取ACCESS_FINE_LOCATION权限")
            } else {
                println("获取ACCESS_FINE_LOCATION权限失败")
            }

            if (it[Manifest.permission.BODY_SENSORS] == true) {
                registerHeartRateListener() //开启运动传感器监听
            } else {
                println("获取BODY_SENSORS权限失败")
            }

            if (it[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                println("成功获取ACCESS_COARSE_LOCATION权限")
            } else {
                println("获取ACCESS_COARSE_LOCATION权限失败")
            }

            if (it[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
                println("成功获取ACTIVITY_RECOGNITION权限")
            } else {
                println("获取ACTIVITY_RECOGNITION权限失败")
            }
        }

    private val heartRateSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.values?.get(0)?.let {
                mBinding.tvHeartRate.text = it.toInt().toString()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }

    }


    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionRequester.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BODY_SENSORS,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            )
        } else {
            permissionRequester.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BODY_SENSORS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            )
        }
    }

    private fun updateTime() {
        calendar.timeInMillis = System.currentTimeMillis()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        if (hour > 12) {
            (hour - 12).let {
                if (it >= 10)
                    timeStringBuilder.append(it)
                else
                    timeStringBuilder.append("0$it")
            }
            timeStringBuilder.append(":")
            if (minute >= 10) {
                timeStringBuilder.append(minute)
            } else {
                timeStringBuilder.append("0$minute")
            }
            timeStringBuilder.append(" PM")
            mBinding.tvTime.text = timeStringBuilder.toString()
            timeStringBuilder.clear()
        } else {
            if (hour >= 10)
                timeStringBuilder.append(hour)
            else
                timeStringBuilder.append("0$hour")
            timeStringBuilder.append(":")
            if (minute >= 10) {
                timeStringBuilder.append(minute)
            } else {
                timeStringBuilder.append("0$minute")
            }
            timeStringBuilder.append(" AM")
            mBinding.tvTime.text = timeStringBuilder.toString()
            timeStringBuilder.clear()
        }
    }

    private fun registerBroadcastReceiver() {
        //注册电池电量广播接收器
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryBroadcastReceiver, intentFilter)
        //注册时间更新广播接收器
        val timeIntentFilter = IntentFilter()
        timeIntentFilter.addAction(Intent.ACTION_TIME_TICK)
        registerReceiver(timeBroadcastReceiver, timeIntentFilter)
    }

    private fun unregisterBroadcastReceiver() {
        unregisterReceiver(batteryBroadcastReceiver)
        unregisterReceiver(timeBroadcastReceiver)
    }

    private fun initObserver() {
        App.instance.eventViewModelStore.batteryViewModel.batteryLevelLiveData.observe(this) {
            mBinding.tvBattery.text = "$it%"
            mBinding.horizontalBattery.power = it
        }
        App.instance.eventViewModelStore.timeViewModel.timeTickLiveData.observe(this) {
            updateTime()
        }
    }

    private fun initListener() {
        mBinding.btnStart.setOnClickListener(object : PreventFastClickListener() {
            override fun onPresentFastClick(v: View?) {
                startActivity(Intent(this@HomeActivity, NewWorkActivity::class.java))
            }
        })
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun startGifAnimation() {
        mViewModel.viewModelScope.launch {
            findViewById<ImageView>(R.id.ivHeartRate).run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(resources, R.mipmap.gif_heart_rate)
                    val drawable = ImageDecoder.decodeDrawable(source)
                    post {
                        setImageDrawable(drawable)
                        if (drawable is AnimatedImageDrawable) {
                            drawable.start()
                        }
                    }
                }
            }
        }
    }

    /**
     * UUID for the Widevine DRM scheme.
     * <p>
     * Widevine is supported on Android devices running Android 4.3 (API Level 18) and up.
     */
    private fun getUniqueId(): String? {
        val uuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        var wvDrm: MediaDrm? = null
        try {
            wvDrm = MediaDrm(uuid)
            val widevineId = wvDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            val md = MessageDigest.getInstance("SHA-256")
            md.update(widevineId)
            return md.digest().toHexString()
        } catch (e: Exception) {
            //WIDEVINE is not available
            return null
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                wvDrm?.close()
            } else {
                wvDrm?.release()
            }
        }
    }


    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    @SuppressLint("HardwareIds")
    private fun initImei() {
        SPUtils.instance.run {
            getString(SPKey.key_imei)?.let {
                mBinding.tvIMEI.text = it
            } ?: kotlin.run {
                try {
                    getSystemService(TelephonyManager::class.java)?.let { telephonyManager ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            telephonyManager.imei.let { imei ->
                                putString(SPKey.key_imei, imei)
                                mBinding.tvIMEI.text = imei
                            }
                        } else {
                            telephonyManager.deviceId.let { imei ->
                                putString(SPKey.key_imei, imei)
                                mBinding.tvIMEI.text = imei
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onSwipeLeft(v: View) {
        startActivity(Intent(this@HomeActivity, ConfigActivity::class.java))
    }

    private fun initSharedPreferenceValue() {
        mViewModel.viewModelScope.launch {
            SPUtils.instance.run {
                if (getString(SPKey.key_ip_address) == null) {
                    putString(SPKey.key_ip_address, "192.168.1.137")
                }
                if (getInt(SPKey.key_port) == null) {
                    putInt(SPKey.key_port, 18891)
                }
                if (getInt(SPKey.key_upload_frequency) == null) {
                    putInt(SPKey.key_upload_frequency, 60)
                }
            }
        }
    }

    private fun initGPSStatus() {
        getSystemService(LocationManager::class.java).let { it ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val gpsStatus = it.getGpsStatus(null)
            println(gpsStatus?.satellites)
            gpsStatus?.satellites?.forEach { gpsSatellite ->
                println("snr: ${gpsSatellite.snr}")
            }
        }
    }
}