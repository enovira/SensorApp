package com.yxh.sensor.mvvm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class HomeActivityViewModel(application: Application) : AndroidViewModel(application) {
    val imeiLiveData: MutableLiveData<String> = MutableLiveData()
}