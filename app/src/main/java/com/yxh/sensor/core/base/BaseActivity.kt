package com.yxh.sensor.core.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<VM : ViewModel, VB : ViewDataBinding> : AppCompatActivity() {

    private var viewModel: VM? = null
    protected val mViewModel: VM get() = viewModel!!
    private var binding: VB? = null
    protected val mBinding: VB get() = binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK)) //设置背景颜色为黑色
        binding = DataBindingUtil.setContentView(this, getLayoutId())
        mBinding.lifecycleOwner = this
        viewModel = createViewModel()
        initView()
    }

    @LayoutRes
    abstract fun getLayoutId(): Int

    abstract fun initView()

    private fun createViewModel(): VM {
        return ViewModelProvider(this)[getJvmClazz(this)]
    }

    @Suppress("UNCHECKED_CAST")
    private fun <VM> getJvmClazz(any: Any): VM {
        return (any::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0] as VM
    }

    override fun onDestroy() {
        binding = null
        viewModel = null
        super.onDestroy()
    }
}