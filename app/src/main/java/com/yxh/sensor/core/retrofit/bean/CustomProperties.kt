package com.yxh.sensor.core.retrofit.bean

class CustomProperties(
    var heartRate: Int?,
    var bloodOxygen: Int?,
    var stepNumber: Int?,
    var position: CustomPosition? = null,
)