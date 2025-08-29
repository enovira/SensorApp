package com.yxh.sensor.mvvm.viewmodel

import android.app.Application
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.yxh.sensor.core.utils.VibrateUtil

class HomeActivityViewModel(private val application: Application) : AndroidViewModel(application) {
    val imeiLiveData: MutableLiveData<String> = MutableLiveData()
    private var vibrator: Vibrator? = null

    private fun initVibrator() {
        vibrator = VibrateUtil.getVibrator(application)
    }

    fun vibrate() {
        if (vibrator == null) {
            initVibrator()
        }
        vibrator?.vibrate(300)
    }

}