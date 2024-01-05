package com.yxh.sensor.mvvm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorkActivityViewModel: ViewModel() {

    /** 心率 */
    var heartRate: MutableLiveData<Int> = MutableLiveData(0)
    /** 血氧 */
    var bloodOxygen: MutableLiveData<Int> = MutableLiveData(0)
    /** 步数 */
    var stepCount: MutableLiveData<Int> = MutableLiveData(0)
}