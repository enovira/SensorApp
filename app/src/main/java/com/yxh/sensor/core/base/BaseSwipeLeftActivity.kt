package com.yxh.sensor.core.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel

abstract class BaseSwipeLeftActivity<VM : ViewModel, VB : ViewDataBinding> :
    BaseActivity<VM, VB>() {

    private var startX = 0f
    private var currentX = 0f
    private var lastX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerSwipeLeftGesture()
    }

    open fun onSwipeLeft(v: View) {
        finish()
    }

    /**
     * 注册左滑退出功能
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun registerSwipeLeftGesture() {
        mBinding.root.setOnTouchListener { v, event ->
            println(lifecycle.currentState)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    currentX = event.rawX
                }

                MotionEvent.ACTION_UP -> {
                    val dx = lastX - startX
//                    println("x: $dx")
                    if (dx > 160) {
                        onSwipeLeft(v)
                    } else {
                        v.x = 0f
                    }
                    lastX = 0f
                }

                MotionEvent.ACTION_MOVE -> {
                    lastX = event.rawX
                    val dx = event.rawX - currentX
                    currentX = event.rawX
                    (v.x + dx).let {
                        v.x = if (it < 0) 0f else it
                    }
                }
            }
            true
        }
    }

}