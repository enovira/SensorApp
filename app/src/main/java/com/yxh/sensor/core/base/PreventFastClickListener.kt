package com.yxh.sensor.core.base

import android.view.View

abstract class PreventFastClickListener: View.OnClickListener {

    private var period = 1000
    private var lastClickTime: Long = 0

    override fun onClick(v: View?) {
        System.currentTimeMillis().let {
            if (it - lastClickTime > period) {
                onPresentFastClick(v)
                lastClickTime = it
            }
        }
    }

    abstract fun onPresentFastClick(v: View?)
}