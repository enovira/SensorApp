package com.yxh.sensor.mvvm.view

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.yxh.sensor.App
import com.yxh.sensor.R
import com.yxh.sensor.core.base.BaseSensorActivity
import com.yxh.sensor.core.global.ConstantStore
import com.yxh.sensor.core.global.SPKey
import com.yxh.sensor.core.receiver.TimeBroadcastReceiver
import com.yxh.sensor.core.retrofit.bean.CustomPosition
import com.yxh.sensor.core.utils.SPUtils
import com.yxh.sensor.databinding.ActivityWorkBinding
import com.yxh.sensor.mvvm.viewmodel.WorkActivityViewModel
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask


class WorkActivity : BaseSensorActivity<WorkActivityViewModel, ActivityWorkBinding>() {

    /** 步数 */
    private var mStepSensor: Sensor? = null

    /** 心率 */
    private var mHeartRateSensor: Sensor? = null

    /** 血氧 */
    private var mBloodOxygenSensor: Sensor? = null

    /** 血压 不可与心率与血氧(其一)同时开启，否则无法获取到血压数据 */
    private var mBloodPressureSensor: Sensor? = null

    private val notificationManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getSystemService(NotificationManager::class.java)
    }
    private val calendar by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Calendar.getInstance() }
    private val timeBroadcastReceiver by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { TimeBroadcastReceiver() }
    private val workTimeStringBuilder = StringBuilder()
    private val timeStringBuilder = StringBuilder()

    private var hour = 0
    private var minute = 0
    private var second = 0
    private var count = 0
    private val period = SPUtils.instance.getInt(SPKey.key_upload_frequency, ConstantStore.defaultFrequency)
    private var latitude: Double? = null
    private var longitude: Double? = null

    private var timer: Timer? = null

    private val channelId: String = "sensorAppId"
    private val channelName: String = "sensorAppChannel"
    private val notificationId: Int = 101
    private var heartRateLevel = 0


    override fun getLayoutId(): Int {
        return R.layout.activity_work
    }

    override fun initView() {
        mBinding.vm = mViewModel
        initListener()
        initReceiver()
        initObserver()
        startTimerTask()
        updateTime()
//        createNotification()
//        startService(Intent(this, WorkService::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_HOME) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startTimerTask() {
        clearTimer()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                count += 1
                if ((count % period) == 0) {
                    customHandler.sendEmptyMessage(1)
                }
                customHandler.sendEmptyMessage(0)
            }
        }, 0, 1000)
    }

    private fun initObserver() {
        App.instance.eventViewModelStore.timeViewModel.timeTickLiveData.observe(this) {
            updateTime()
        }
        App.instance.eventViewModelStore.sensorEventViewModel.timeCountLiveData.observe(this) {
            if ((it % period) == 0) {
                customHandler.sendEmptyMessage(1)
            }
            customHandler.sendEmptyMessage(0)
            updateWorkTime()
        }
    }

    private fun initReceiver() {
        //注册时间更新广播接收器
        val timeIntentFilter = IntentFilter()
        timeIntentFilter.addAction(Intent.ACTION_TIME_TICK)
        registerReceiver(timeBroadcastReceiver, timeIntentFilter)
    }

    private fun initListener() {
        mBinding.tvFinish.setOnClickListener {
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onSwipeLeft(mBinding.root)
            }
        })
    }

    /**
     * 记录工作时间
     */
    private fun updateWorkTime() {
        second += 1
        if (second >= 60) {
            minute += 1
            second = 0
            if (minute >= 60) {
                minute = 0
                hour += 1
            }
        }
        if (hour < 10)
            workTimeStringBuilder.append("0$hour")
        else
            workTimeStringBuilder.append(hour)
        workTimeStringBuilder.append(":")
        if (minute < 10)
            workTimeStringBuilder.append("0$minute")
        else
            workTimeStringBuilder.append(minute)
        workTimeStringBuilder.append(":")
        if (second < 10)
            workTimeStringBuilder.append("0$second")
        else
            workTimeStringBuilder.append(second)
        mBinding.tvWorkTime.text = workTimeStringBuilder.toString()
        workTimeStringBuilder.clear()
    }

    override fun setSensorListener() {
        mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) //步数
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) //心率
        mBloodOxygenSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT) //血氧
//        mBloodPressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) //血压

        mSensorManager.registerListener(
            sensorEventListener,
            mStepSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        mSensorManager.registerListener(
            sensorEventListener,
            mHeartRateSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        mSensorManager.registerListener(
            sensorEventListener,
            mBloodOxygenSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

//        mSensorManager.registerListener(sensorEventListener, mBloodPressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun setLocationListener() {
        val locationManager = getSystemService(LocationManager::class.java)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
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
        val providers = locationManager.allProviders
        var provider: String = LocationManager.GPS_PROVIDER
        if (providers.size == 0) {
            return
        } else {
            for (providerName in providers) {
                if (locationManager.isProviderEnabled(providerName)) {
                    provider = providerName
                    break
                }
            }
        }
        for (provider1 in locationManager.getProviders(true)) {
            println("provider: $provider1 is enabled = ${locationManager.isProviderEnabled(provider1)}")
            println(Gson().toJson(locationManager.getLastKnownLocation(provider1)))
        }

        locationManager.getLastKnownLocation(provider)?.let {
            latitude = it.latitude
            longitude = it.longitude
            Log.d("Location", "经度: ${it.latitude}，纬度: ${it.longitude}")
        }

        locationManager.requestLocationUpdates(provider, 10000, 0f, locationListener)
    }

    override fun unRegisterSensorListener() {
        mSensorManager.unregisterListener(sensorEventListener)
    }


    /**
     * 传感器数据监听
     */
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    println("心率: ${Gson().toJson(event.values)}")
                    event.values[0].toInt().let {
                        mViewModel.heartRate.postValue(it)
                        val level = if (it in 110..119) {
                            1
                        } else if (it in 120..129) {
                            2
                        } else if (it in 130..139) {
                            3
                        } else if (it in 140..149) {
                            4
                        }  else if (it >= 150) {
                            5
                        } else {
                            0
                        }
                        judgeHeartRateLevelChanged(level)
                    }
                }

                Sensor.TYPE_STEP_COUNTER -> {
                    println("步数: ${Gson().toJson(event.values)}")
                    mViewModel.stepCount.postValue(event.values?.get(0)?.toInt())
                }

                Sensor.TYPE_HEART_BEAT -> {
                    println("血氧: ${Gson().toJson(event.values)}")
                    mViewModel.bloodOxygen.postValue(event.values[1].toInt())
                }
//                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
//                    println("血压: ${Gson().toJson(event.values)}")
//                    var systolic = event.values[0].toInt()
//                    var diastolic = event.values[1].toInt()
//                    if (diastolic > 0 && systolic > 0) {
//                        if (diastolic < 60) {
//                            diastolic = 60
//                        }
//                        if (diastolic > 85) {
//                            diastolic = 85
//                        }
//                        if (systolic > 100) {
//                            systolic = 100
//                        }
//                        if (systolic < 60) {
//                            systolic = 60
//                        }
//                        systolic += 45
//                        mBinding.tvBloodPressure.text = " 收缩压：$systolic 舒张压：$diastolic"
//                    }
//                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    private val gestureDetectorListener = object : SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            e1?.let {
                if (e2.x - it.x > 100) {
                    finish()
                }
            }
            return true
        }

    }

    private fun judgeHeartRateLevelChanged(level: Int) {
        if (level != heartRateLevel) {
            heartRateLevel = level
            when(level) {
                0 -> mBinding.ivHeartRateLevel.setImageResource(R.mipmap.ic_heart_rate_level_0)
                1 -> mBinding.ivHeartRateLevel.setImageResource(R.mipmap.ic_heart_rate_level_1)
                2 -> mBinding.ivHeartRateLevel.setImageResource(R.mipmap.ic_heart_rate_level_2)
                3 -> mBinding.ivHeartRateLevel.setImageResource(R.mipmap.ic_heart_rate_level_3)
                4 -> mBinding.ivHeartRateLevel.setImageResource(R.mipmap.ic_heart_rate_level_4)
                5 -> mBinding.ivHeartRateLevel.setImageResource(R.mipmap.ic_heart_rate_level_5)
            }
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            latitude = location.latitude
            longitude = location.longitude
            println("经度: $latitude, 纬度: $longitude")
        }

        override fun onProviderEnabled(provider: String) {
            println("位置信息已开启")
        }

        override fun onProviderDisabled(provider: String) {
            println("位置信息已关闭")
        }
    }

    override fun onSwipeLeft(v: View) {
        v.x = 0f
        if (mBinding.llMarks.visibility == View.VISIBLE) {
            mBinding.llMarks.visibility = View.GONE
        } else {
            mBinding.llMarks.visibility = View.VISIBLE
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

    override fun onDestroy() {
        clearTimer()
        customHandler.removeCallbacksAndMessages(null)
        unregisterReceiver(timeBroadcastReceiver)
        notificationManager.cancel(notificationId)
        super.onDestroy()
    }

    private fun clearTimer() {
        timer?.cancel()
        timer = null
    }

    inner class CustomHandler(looper: Looper, callback: Callback) : Handler(looper, callback)

    private val customHandler = CustomHandler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            0 -> {
                updateWorkTime()
            }
            1 -> {
                kotlin.runCatching {
                    mViewModel.reportProperties(CustomPosition(latitude, longitude))
                }.exceptionOrNull()?.run {
                    printStackTrace()
                    Toast.makeText(this@WorkActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        true
    }

}