package com.yxh.sensor.core.utils

import android.content.Context
import android.os.Vibrator

object VibrateUtil {

    fun getVibrator(context: Context): Vibrator {
        return context.getSystemService(Vibrator::class.java)
    }

}