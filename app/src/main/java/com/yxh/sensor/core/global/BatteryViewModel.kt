package com.yxh.sensor.core.global

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

object BatteryViewModel: ViewModel() {
    val batteryLevelLiveData = MutableLiveData<Int>()
}