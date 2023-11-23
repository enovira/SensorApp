package com.yxh.sensor.core.global

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

object TimeViewModel: ViewModel() {
    /** 时间更新广播 */
    val timeTickLiveData = MutableLiveData<Boolean>()
}