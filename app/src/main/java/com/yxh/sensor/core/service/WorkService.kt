package com.yxh.sensor.core.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.alibaba.fastjson.JSONObject
import com.blankj.utilcode.util.LogUtils
import com.yxh.sensor.App
import com.yxh.sensor.R
import com.yxh.sensor.core.global.ConstantStore
import com.yxh.sensor.core.global.SPKey
import com.yxh.sensor.core.provider.CustomLocationProvider
import com.yxh.sensor.core.provider.CustomSensorProvider
import com.yxh.sensor.core.receiver.AlarmTaskReceiver
import com.yxh.sensor.core.retrofit.bean.CustomPosition
import com.yxh.sensor.core.utils.SPUtils
import com.yxh.sensor.mvvm.view.WorkActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class WorkService : Service() {

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
        setSensorListener()
        val intentFilter = IntentFilter()
        intentFilter.addAction("sensorWakeupBroadcast")
        ContextCompat.registerReceiver(this, wakeupBroadcastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        period = SPUtils.instance.getInt(SPKey.key_upload_frequency, ConstantStore.defaultFrequency)
        startTimer(period)
        setAlarm()
        createNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        CustomLocationProvider.getInstance().unregisterLocationCallback()
        CustomSensorProvider.getInstance().unregisterSensorCallback()
        notificationManager.cancel(notificationId)
        unregisterReceiver(wakeupBroadcastReceiver)
        timer?.cancel()
        timer = null
        super.onDestroy()
    }

    private fun startTimer(internal: Int) {
        timer?.cancel()
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    reportProperty()
                }
            }, 10 * 1000, (1000 * internal).toLong())
        }
    }

    /**
     * 创建通知
     */
    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel) // 创建完通知通道后需在NotificationManager中创建通道
        }
        NotificationCompat.WearableExtender()
        // channelId需要与步骤2中的channelId一致
        val notification = NotificationCompat.Builder(this@WorkService, channelId)
            .setSmallIcon(R.mipmap.icon_launcher) //设置通知的图标
            .setContentTitle(getString(R.string.app_name)) //设置标题
            .setContentText("正在工作中，回到工作") //消息内容
//            .setDefaults(Notification.DEFAULT_ALL) //设置默认的提示音
            .setOngoing(true) //让通知左右滑的时候不能取消通知
//            .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知的优先级
            .setWhen(System.currentTimeMillis()) //设置通知时间，默认为系统发出通知的时间，通常不用设置
            .setAutoCancel(false) //打开程序后图标消失
            .apply {
                val contentIntent = Intent(this@WorkService, WorkActivity::class.java)
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
    private fun setSensorListener() {
        CustomSensorProvider.getInstance().registerHeartRateCallback {
            App.instance.eventViewModelStore.sensorEventViewModel.heartRate.postValue(it)
            println("心率: $it")
        }.registerBloodOxygenCallback {
            App.instance.eventViewModelStore.sensorEventViewModel.bloodOxygen.postValue(it)
            println("血氧: $it")
        }.registerStepCountCallback {
            App.instance.eventViewModelStore.sensorEventViewModel.stepCount.postValue(it)
            println("步数: $it")
        }.initialize(this)
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
        CustomLocationProvider.getInstance().registerLocationCallback(this) {
            latitude = it.latitude
            longitude = it.longitude
        }
    }

    inner class WakeupBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action?.equals("sensorWakeupBroadcast") == true) {
                setAlarm()
                wakeup(30 * 1000)
//                setSensorListener(30 * 1000)
//                reportProperty()
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
        calendar.add(Calendar.MINUTE, 5) //设置唤醒间隔,五分钟唤醒一次
        val intent = Intent(this, AlarmTaskReceiver::class.java)
        intent.action = "alarmBroadcast"
//        intent.component = ComponentName(packageName, WorkService::class.java.name)
        val pendingIntent =
            PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_IMMUTABLE)
        // 检查是否可以设置精确闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                // 如果不能设置精确闹钟，使用普通闹钟
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else {
            // Android 12以下版本直接设置精确闹钟
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }
}