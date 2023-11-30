package com.yxh.sensor.mvvm.view

import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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

        mBinding.llFrequency.setOnClickListener {
            if (!it.isSelected) {
                switchEdittextEnabled(mBinding.etFrequency, true)
                switchEdittextEnabled(mBinding.etIp, false)
                switchEdittextEnabled(mBinding.etPort, false)

                it.isSelected = true
                mBinding.llIp.isSelected = false
                mBinding.llPort.isSelected = false
                toggleInputMethod(it, false)
            } else {
                mBinding.etFrequency.run {
                    if (isFocusable && isEnabled) {
                        requestFocus()
                        toggleInputMethod(this, true)
                    }
                }
            }
        }

        mBinding.llIp.setOnClickListener {
            if (!it.isSelected) {
                switchEdittextEnabled(mBinding.etFrequency, false)
                switchEdittextEnabled(mBinding.etIp, true)
                switchEdittextEnabled(mBinding.etPort, false)

                it.isSelected = true
                mBinding.llFrequency.isSelected = false
                mBinding.llPort.isSelected = false
                toggleInputMethod(it, false)
            } else {
                mBinding.etIp.run {
                    if (isFocusable && isEnabled) {
                        requestFocus()
                        toggleInputMethod(this, true)
                    }
                }
            }
        }

        mBinding.llPort.setOnClickListener {
            if (!it.isSelected) {
                switchEdittextEnabled(mBinding.etFrequency, false)
                switchEdittextEnabled(mBinding.etIp, false)
                switchEdittextEnabled(mBinding.etPort, true)

                it.isSelected = true
                mBinding.llFrequency.isSelected = false
                mBinding.llIp.isSelected = false
                toggleInputMethod(it, false)
            } else {
                mBinding.etPort.run {
                    if (isFocusable && isEnabled) {
                        requestFocus()
                        toggleInputMethod(this, true)
                    }
                }
            }
        }

        mBinding.etIp.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.text.trim().toString().let {
                    if (TextUtils.isEmpty(it)) {
                        Toast.makeText(this@ConfigActivity, "IP地址不能为空", Toast.LENGTH_SHORT)
                            .show()
                    } else if (!judgeIpAvailable(it)) {
                        Toast.makeText(this@ConfigActivity, "IP地址不符合规范", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        SPUtils.instance.putString(SPKey.key_ip_address, it)
                    }
                }
            }
            false
        }
        mBinding.etFrequency.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.text.trim().toString().toIntOrNull()?.let {
                    if (it < 60) {
                        Toast.makeText(
                            this@ConfigActivity,
                            "上传频率(间隔)不能低于60秒",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        SPUtils.instance.putInt(SPKey.key_upload_frequency, it)
                        v.text = it.toString()
                    }
                } ?: kotlin.run {
                    Toast.makeText(this@ConfigActivity, "上传频率不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            false
        }
        mBinding.etPort.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.text.trim().toString().toIntOrNull()?.let {
                    if (it < 0 || it > 65535) {
                        Toast.makeText(
                            this@ConfigActivity,
                            "请输入平台端口号(0~65536)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        SPUtils.instance.putInt(SPKey.key_port, it)
                        v.text = it.toString()
                    }
                } ?: kotlin.run {
                    Toast.makeText(
                        this@ConfigActivity,
                        "请输入平台端口号(0~65536)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            false
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

    private fun switchEdittextEnabled(editText: EditText, enabled: Boolean) {
        editText.run {
            isEnabled = enabled
//            isClickable = enabled
//            isFocusable = enabled
        }
    }
}