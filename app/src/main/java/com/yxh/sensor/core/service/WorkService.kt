package com.yxh.sensor.core.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
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
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.view.KeyEventDispatcher.Component
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.yxh.sensor.App
import com.yxh.sensor.R
import com.yxh.sensor.core.global.ConstantStore
import com.yxh.sensor.core.global.SPKey
import com.yxh.sensor.core.global.SensorEventViewModel
import com.yxh.sensor.core.receiver.AlarmTaskReceiver
import com.yxh.sensor.core.retrofit.bean.CustomPosition
import com.yxh.sensor.core.utils.SPUtils
import com.yxh.sensor.mvvm.view.NewWorkActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class WorkService : Service(), SensorEventListener, LocationListener {

    private val sensorManager: SensorManager by lazy { getSystemService(SensorManager::class.java) }
    private val locationManager: LocationManager by lazy { getSystemService(LocationManager::class.java) }
    private val notificationManager: NotificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val alarmManager: AlarmManager by lazy { getSystemService(AlarmManager::class.java) }

    private val channelId = "SensorAppChannelId"
    private val channelName = "SensorAppChannelName"
    private val notificationId = 106

    private var period: Int = 60

    private var latitude = 0.0
    private var longitude = 0.0
    private var mWakeLock: WakeLock? = null

    private val wakeupBroadcastReceiver = WakeupBroadcastReceiver()
    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

    private var timer: Timer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        setLocationListener()
        setSensorListener(30)
        val intentFilter = IntentFilter()
        intentFilter.addAction("sensorWakeupBroadcast")
        registerReceiver(wakeupBroadcastReceiver, intentFilter)
        startTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        period = SPUtils.instance.getInt(SPKey.key_upload_frequency, ConstantStore.defaultFrequency)
        setAlarm()
        createNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        locationManager.removeUpdates(this)
        sensorManager.unregisterListener(this)
        notificationManager.cancel(notificationId)
        unregisterReceiver(wakeupBroadcastReceiver)
        timer?.cancel()
        timer = null
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    sensorEvent.values[0].toInt().let {
                        App.instance.eventViewModelStore.sensorEventViewModel.heartRate.postValue(it)
                        println("心率: $it")
                    }
                }

                Sensor.TYPE_STEP_COUNTER -> {
                    sensorEvent.values?.get(0)?.toInt()?.let {
                        App.instance.eventViewModelStore.sensorEventViewModel.stepCount.postValue(it)
                        println("步数: $it")
                    }
                }

                Sensor.TYPE_HEART_BEAT -> {
                    sensorEvent.values[1].toInt().let {
                        if (it > 0) {
                            App.instance.eventViewModelStore.sensorEventViewModel.bloodOxygen.postValue(it)
                        }
                        println("血氧: $it")
                    }
                }

                else -> {}
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        println("经度: $latitude, 纬度: $longitude")
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer().apply {
            schedule(object : java.util.TimerTask() {
                override fun run() {
                    LogUtils.d("time: ${simpleDateFormat.format(System.currentTimeMillis())}")
                }
            }, 0, 1000)
        }
    }

    /**
     * 创建通知
     */
    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(notificationChannel) // 创建完通知通道后需在NotificationManager中创建通道
        }
        // channelId需要与步骤2中的channelId一致
        val notification = NotificationCompat.Builder(this@WorkService, channelId)
            .setSmallIcon(R.mipmap.step_icon) //设置通知的图标
            .setContentTitle("SensorApp") //设置标题
            .setContentText("正在工作中，回到工作") //消息内容
//            .setDefaults(Notification.DEFAULT_ALL) //设置默认的提示音
            .setOngoing(false) //让通知左右滑的时候不能取消通知
//            .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知的优先级
            .setWhen(System.currentTimeMillis()) //设置通知时间，默认为系统发出通知的时间，通常不用设置
            .setAutoCancel(false) //打开程序后图标消失
            .apply {
                val contentIntent = Intent(this@WorkService, NewWorkActivity::class.java)
                // 第四个参数至关重要，参考android基础intent篇中的pendingIntent详解
                val pendingIntent = PendingIntent.getActivity(
                    this@WorkService,
                    200,
                    contentIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                // 设置点击前台通知后进行的动作,此处示例为启动服务
                setContentIntent(pendingIntent)
            }.build()
        notificationManager.notify(notificationId, notification)
        startForeground(notificationId, notification)
    }

    /**
     * 设置健康状态监听器
     */
    private fun setSensorListener(timeout: Long) {
//        customHandler.removeMessages(CANCEL_MEASURE)
//        customHandler.sendEmptyMessageDelayed(CANCEL_MEASURE, timeout) //延迟一段时间后取消健康测量

        val mStepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) //步数
        val mHeartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) //心率
        val mBloodOxygenSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT) //血氧

        sensorManager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, mBloodOxygenSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    /**
     * 设置位置监听器
     */
    private fun setLocationListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
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
        locationManager.requestLocationUpdates(provider, 30 * 1000, 0f, this)
    }

    inner class WakeupBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action?.equals("sensorWakeupBroadcast") == true) {
                setAlarm()
                wakeup(30 * 1000)
//                setSensorListener(30 * 1000)
                reportProperty()
            }
        }
    }

    /**
     * 上送数据
     */
    private fun reportProperty() {
        kotlin.runCatching {
            App.instance.eventViewModelStore.sensorEventViewModel.reportProperties(
                CustomPosition(latitude, longitude))
        }.exceptionOrNull()?.run {
            LogUtils.e(message)
            App.instance.eventViewModelStore.sensorEventViewModel.apiServerException.postValue(
                message
            )
        }
    }

    private fun wakeup(duration: Long) {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (mWakeLock == null) {
            mWakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "AlarmTaskReceiver:wakeup"
            )
        }
        mWakeLock?.let {
            if (!it.isHeld) {
                it.acquire(duration) //获取具有超时时间的唤醒锁，超时后自动释放
            }
        }
    }

    private fun setAlarm() {
        LogUtils.d("setAlarm ${simpleDateFormat.format(System.currentTimeMillis())}")
        val calendar = Calendar.getInstance(Locale.CHINA)
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.SECOND, period) //设置唤醒间隔
        val intent = Intent(this, AlarmTaskReceiver::class.java)
        intent.action = "alarmBroadcast"
//        intent.component = ComponentName(packageName, WorkService::class.java.name)
        val pendingIntent =
            PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    companion object {
        private const val CANCEL_MEASURE = 1
        class CustomHandler(looper: Looper, callback: Callback) : Handler(looper, callback)
    }

    private val customHandler = CustomHandler(Looper.getMainLooper()) {
        if (it.what == CANCEL_MEASURE) {
            sensorManager.unregisterListener(this)
        }
        true
    }
}