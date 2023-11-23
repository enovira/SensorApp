package com.yxh.sensor.core.base

import android.view.View

fun View.setPreventFastClickListener(listener: PreventFastClickListener) {
    setOnClickListener(listener)
}