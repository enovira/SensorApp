package com.yxh.sensor.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yxh.sensor.App

class BatteryBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            when(it.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    it.getIntExtra("level", -1).let { level ->
                        if (level != -1) {
                            println("接收到电池变化信息了=> level: $level")
                            App.instance.eventViewModelStore.batteryViewModel.batteryLevelLiveData.postValue(level)
                        }
                    }
                }

                else -> {}
            }
        }
    }
}