package com.yxh.sensor.core.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

typealias StepCountCallback = (Int) -> Unit
typealias HeartRateCallback = (Int) -> Unit
typealias BloodOxygenCallback = (Int) -> Unit

class CustomSensorProvider private constructor() : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var heartRateSensor: Sensor? = null
    private var heartBeatSensor: Sensor? = null

    // 回调接口
    private var stepCountCallback: StepCountCallback? = null
    private var heartRateCallback: HeartRateCallback? = null
    private var bloodOxygenCallback: BloodOxygenCallback? = null

    // 定时任务执行器
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // 上次检测时间
    private var lastHeartRateTime: Long = 0
    private var lastBloodOxygenTime: Long = 0

    // 检测间隔（毫秒）
    private var heartRateInterval = 60 * 1000L // 1分钟
    private var bloodOxygenInterval = 300 * 1000L // 5分钟
    private var stepCountInternal = 30 * 1000L // 30秒

    companion object {

        private var instance: CustomSensorProvider? = null

        @JvmStatic
        fun getInstance(): CustomSensorProvider {
            return instance ?: synchronized(this) {
                instance ?: CustomSensorProvider().also { instance = it }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                // 步数数据
                val stepCount = event.values[0].toInt()
                stepCountCallback?.invoke(stepCount)
            }

            Sensor.TYPE_HEART_RATE -> {
                // 心率数据
                val heartRate = event.values[0].toInt()
                if (heartRate > 0) { // 过滤无效数据
                    heartRateCallback?.invoke(heartRate)
                }
            }

            Sensor.TYPE_HEART_BEAT -> {
                // 血氧数据（通常在values[1]中）
                if (event.values.size > 1) {
                    val bloodOxygen = event.values[1].toInt()
                    if (bloodOxygen > 0) { // 过滤无效数据
                        bloodOxygenCallback?.invoke(bloodOxygen)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 传感器精度变化时的处理
    }


    /**
     * 初始化传感器
     */
    fun initialize(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // 检查传感器权限
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 权限不足，可以在UI层提示用户授予权限
            return
        }

        // 获取步数传感器
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // 获取心率传感器
        heartRateSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        // 获取心跳传感器（用于血氧检测）
        heartBeatSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_BEAT)
    }

    fun registerHeartRateCallback(callback: HeartRateCallback): CustomSensorProvider {
        heartRateCallback = callback
        return this
    }

    fun registerStepCountCallback(callback: StepCountCallback): CustomSensorProvider {
        stepCountCallback = callback
        return this
    }

    fun registerBloodOxygenCallback(callback: BloodOxygenCallback): CustomSensorProvider {
        bloodOxygenCallback = callback
        return this
    }

    fun unregisterSensorCallback() {
        stopDetection()
        stepCountCallback = null
        heartRateCallback = null
        bloodOxygenCallback = null
        sensorManager?.unregisterListener(this)
        sensorManager = null
    }

    /**
     * 开始周期性检测
     */
    fun startPeriodicDetection() {
        // 取消之前的任务
        scheduler.shutdownNow()

        // 创建新的调度器
        val newScheduler = Executors.newSingleThreadScheduledExecutor()

        detectStepCount()

        //30秒检测一次步数
//        newScheduler.scheduleWithFixedDelay({
//            detectStepCount()
//        }, 0, stepCountInternal, TimeUnit.MILLISECONDS)

        // 定期检测心率
        newScheduler.scheduleWithFixedDelay({
            if (shouldDetectHeartRate()) {
                detectHeartRate()
            }
        }, 0, heartRateInterval, TimeUnit.MILLISECONDS)

        // 定期检测血氧
        newScheduler.scheduleWithFixedDelay({
            if (shouldDetectBloodOxygen()) {
                detectBloodOxygen()
            }
        }, 30000, bloodOxygenInterval, TimeUnit.MILLISECONDS) // 延迟30秒开始
    }

    private fun detectStepCount() {
        stepSensor?.let { sensor ->
            sensorManager?.let { manager ->
                manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                // 10秒后停止监听以节省电量
//                mainHandler.postDelayed({
//                    manager.unregisterListener(this, sensor)
//                }, 10 * 1000)
            }
        }
    }

    /**
     * 检测心率
     */
    private fun detectHeartRate() {
        heartRateSensor?.let { sensor ->
            sensorManager?.let { manager ->
                manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                // 30秒后停止监听以节省电量,至少需要10秒才能检测到心跳
                mainHandler.postDelayed({
                    manager.unregisterListener(this, sensor)
                }, 30 * 1000)
            }
        }
        lastHeartRateTime = System.currentTimeMillis()
    }

    /**
     * 检测血氧
     */
    private fun detectBloodOxygen() {
        heartBeatSensor?.let { sensor ->
            sensorManager?.let { manager ->
                manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                // 20秒后停止监听以节省电量
                mainHandler.postDelayed({
                    manager.unregisterListener(this, sensor)
                }, 20000)
            }
        }
        lastBloodOxygenTime = System.currentTimeMillis()
    }

    /**
     * 检查是否应该检测心率
     */
    private fun shouldDetectHeartRate(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastHeartRateTime) >= heartRateInterval
    }

    /**
     * 检查是否应该检测血氧
     */
    private fun shouldDetectBloodOxygen(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastBloodOxygenTime) >= bloodOxygenInterval
    }

    /**
     * 停止所有检测
     */
    fun stopDetection() {
        scheduler.shutdownNow()
        sensorManager?.unregisterListener(this)
    }

}