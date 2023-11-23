package com.yxh.sensor.core.global

import androidx.lifecycle.ViewModel

class EventViewModelStore: ViewModel() {
    var batteryViewModel = BatteryViewModel
    var timeViewModel = TimeViewModel
    var sensorEventViewModel = SensorEventViewModel
}