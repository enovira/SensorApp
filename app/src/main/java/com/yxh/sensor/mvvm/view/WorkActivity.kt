package com.yxh.sensor.mvvm.view

import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.yxh.sensor.App
import com.yxh.sensor.R
import com.yxh.sensor.core.base.BaseSwipeLeftActivity
import com.yxh.sensor.core.receiver.TimeBroadcastReceiver
import com.yxh.sensor.core.service.WorkService
import com.yxh.sensor.databinding.ActivityWorkBinding
import com.yxh.sensor.mvvm.viewmodel.WorkActivityViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


class WorkActivity : BaseSwipeLeftActivity<WorkActivityViewModel, ActivityWorkBinding>() {

    private val calendar by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Calendar.getInstance() }
    private val timeBroadcastReceiver by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { TimeBroadcastReceiver() }
    private val workTimeStringBuilder = StringBuilder()
    private val timeStringBuilder = StringBuilder()
    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

    private var hour = 0
    private var minute = 0
    private var second = 0

    private var heartRateLevel = 0

    private var startTimeMillis = System.currentTimeMillis() //开始工作时间
    private var timer: Timer? = null

    override fun getLayoutId(): Int {
        return R.layout.activity_work
    }

    override fun initView() {
        mBinding.vm = mViewModel
        initListener()
        initReceiver()
        initObserver()
        updateTime()

        startService(Intent(this, WorkService::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_HOME) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun initObserver() {
        App.instance.eventViewModelStore.timeViewModel.timeTickLiveData.observe(this) {
            updateTime()
        }
        App.instance.eventViewModelStore.sensorEventViewModel.heartRate.observe(this) {
            mViewModel.heartRate.postValue(it)
            judgeHeartRateLevelChanged(if (it in 110..119) {
                1
            } else if (it in 120..129) {
                2
            } else if (it in 130..139) {
                3
            } else if (it in 140..149) {
                4
            } else if (it >= 150) {
                5
            } else {
                0
            })
        }
        App.instance.eventViewModelStore.sensorEventViewModel.bloodOxygen.observe(this) {
            mViewModel.bloodOxygen.postValue(it)
        }
        App.instance.eventViewModelStore.sensorEventViewModel.stepCount.observe(this) {
            mViewModel.stepCount.postValue(it)
        }
        App.instance.eventViewModelStore.sensorEventViewModel.apiServerException.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
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
            stopService(Intent(this, WorkService::class.java))
        }

        mBinding.llMarks.setOnClickListener {
            it.visibility = View.GONE
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
    private fun updateWorkTime(time: Int) {
        hour = time / 3600
        minute = (time % 3600) / 60
        second = time % 60
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

    override fun onResume() {
        startTimer()
        super.onResume()
    }

    override fun onPause() {
        clearTimer()
        super.onPause()
    }

    private fun startTimer() {
        clearTimer()
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    workActivityHandler.sendEmptyMessage(0)
                }
            }, 0, 1000)
        }
    }

    private fun clearTimer() {
        timer?.cancel()
        timer = null
    }

    override fun onDestroy() {
        unregisterReceiver(timeBroadcastReceiver)
        stopService(Intent(this, WorkService::class.java))
        super.onDestroy()
    }

    companion object {
        class WorkActivityHandler(looper: Looper, callback: Callback): Handler(looper, callback)
    }

    private val workActivityHandler = WorkActivityHandler(Looper.getMainLooper()) {
        if (it.what == 0) {
            updateWorkTime(((System.currentTimeMillis() - startTimeMillis) / 1000).toInt())
        }
        true
    }

}