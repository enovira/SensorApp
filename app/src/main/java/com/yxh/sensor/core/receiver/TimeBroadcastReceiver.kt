package com.yxh.sensor.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yxh.sensor.App

class TimeBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            when(it.action) {
                Intent.ACTION_TIME_TICK -> {
                    println("接收到时间变化广播了")
                    App.instance.eventViewModelStore.timeViewModel.timeTickLiveData.postValue(true)
                }
            }
        }
    }
}