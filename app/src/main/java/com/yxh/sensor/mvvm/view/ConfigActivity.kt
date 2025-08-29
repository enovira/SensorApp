package com.yxh.sensor.mvvm.view

import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.yxh.sensor.R
import com.yxh.sensor.core.base.BaseSwipeLeftActivity
import com.yxh.sensor.core.global.ConstantStore
import com.yxh.sensor.core.global.SPKey
import com.yxh.sensor.core.utils.SPUtils
import com.yxh.sensor.databinding.ActivityConfigBinding
import com.yxh.sensor.mvvm.viewmodel.ConfigActivityViewModel

class ConfigActivity : BaseSwipeLeftActivity<ConfigActivityViewModel, ActivityConfigBinding>() {

    private val inputMethodManager by lazy {
        getSystemService(InputMethodManager::class.java)
    }


    override fun getLayoutId(): Int {
        return R.layout.activity_config
    }

    override fun initView() {
        mBinding.vm = mViewModel
        initListener()
        initSharedPreferenceValue()
    }

    private fun initListener() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        mBinding.etFrequency.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                mBinding.llFrequency.isSelected = true
                mBinding.llIp.isSelected = false
                mBinding.llPort.isSelected = false
                toggleInputMethod(v, true)
            } else {
                judgeFrequencyValid(mBinding.etFrequency.text.toString().trim().toIntOrNull())
                toggleInputMethod(v, false)
            }
        }
        mBinding.etFrequency.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }
        mBinding.llFrequency.setOnClickListener {
            mBinding.etFrequency.requestFocus()
        }

        mBinding.etIp.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                mBinding.llFrequency.isSelected = false
                mBinding.llIp.isSelected = true
                mBinding.llPort.isSelected = false
                toggleInputMethod(v, true)
            } else {
                judgeIpValid(mBinding.etIp.text.toString().trim())
                toggleInputMethod(v, false)
            }
        }
        mBinding.etIp.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }
        mBinding.llIp.setOnClickListener {
            mBinding.etIp.requestFocus()
        }

        mBinding.etPort.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                mBinding.llFrequency.isSelected = false
                mBinding.llIp.isSelected = false
                mBinding.llPort.isSelected = true
                toggleInputMethod(v, true)
            } else {
                judgePortValid(mBinding.etPort.text.toString().trim().toIntOrNull())
                toggleInputMethod(v, false)
            }
        }
        mBinding.etPort.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }
        mBinding.llPort.setOnClickListener {
            mBinding.etPort.requestFocus()
        }
    }
    private fun hideKeyboard() {
        val currentFocus = currentFocus
        if (currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(
                currentFocus.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    override fun finish() {
        if (!isFinishing) {
            super.finish()
        }
    }

    private fun initSharedPreferenceValue() {
        SPUtils.instance.run {
            mBinding.etFrequency.setText(getInt(SPKey.key_upload_frequency, ConstantStore.defaultFrequency).toString())
            mBinding.etIp.setText(getString(SPKey.key_ip_address, ConstantStore.defaultIp))
            mBinding.etPort.setText(getInt(SPKey.key_port, ConstantStore.defaultPort).toString())
        }
    }

    private fun judgeIpAvailable(ip: String): Boolean {
        val regexStr = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
        val regex = Regex(regexStr)
        return regex.matches(ip)
    }

    private fun toggleInputMethod(v: View, state: Boolean) {
        if (state) {
            inputMethodManager.showSoftInput(v, InputMethodManager.HIDE_IMPLICIT_ONLY)
        } else {
            inputMethodManager.hideSoftInputFromWindow(
                v.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    private fun judgeFrequencyValid(frequency: Int?) {
        if (frequency == null) {
            Toast.makeText(this@ConfigActivity, "上传频率不能为空", Toast.LENGTH_SHORT).show()
        } else {
            if (frequency < ConstantStore.highestFrequency) {
                Toast.makeText(this@ConfigActivity, "上传频率不能高于1次/${ConstantStore.highestFrequency}秒", Toast.LENGTH_SHORT).show()
            } else {
                SPUtils.instance.putInt(SPKey.key_upload_frequency, frequency)
                mBinding.etFrequency.setText(frequency.toString())
                Toast.makeText(this, "上传频率更新成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun judgeIpValid(ip: String) {
        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(this@ConfigActivity, "IP地址不能为空", Toast.LENGTH_SHORT).show()
        } else if (!judgeIpAvailable(ip)) {
            Toast.makeText(this@ConfigActivity, "IP地址不符合规范", Toast.LENGTH_SHORT).show()
        } else {
            SPUtils.instance.putString(SPKey.key_ip_address, ip)
            mBinding.etIp.setText(ip)
            Toast.makeText(this, "IP地址更新成功", Toast.LENGTH_SHORT).show()
        }
    }

    private fun judgePortValid(port: Int?) {
        port?.let {
            if (it < 0 || it > 65535) {
                Toast.makeText(this@ConfigActivity, "请输入平台端口号(0~65536)", Toast.LENGTH_SHORT).show()
            } else {
                SPUtils.instance.putInt(SPKey.key_port, it)
                mBinding.etPort.setText(it.toString())
                Toast.makeText(this, "平台端口号更新成功", Toast.LENGTH_SHORT).show()
            }
        } ?: kotlin.run {
            Toast.makeText(this@ConfigActivity, "请输入平台端口号(0~65536)", Toast.LENGTH_SHORT).show()
        }
    }
}