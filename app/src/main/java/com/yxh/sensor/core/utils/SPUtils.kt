package com.yxh.sensor.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.yxh.sensor.App

class SPUtils() {


    companion object {
        private val name = "sensorApp"
        private var sharedPreferences: SharedPreferences =
            App.instance.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        private var editor: Editor = sharedPreferences.edit()

        val instance: SPUtils by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            SPUtils()
        }
    }

    fun putInt(key: String ,value: Int) {
        editor.putInt(key, value)
        editor.apply()
    }

    fun putString(key: String ,value: String) {
        editor.putString(key, value)
        editor.apply()
    }

    fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun getString(key: String, defvalue: String?): String? {
        return sharedPreferences.getString(key, defvalue)
    }

    fun getInt(key: String, defvalue: Int): Int {
        return sharedPreferences.getInt(key, defvalue)
    }

    fun getInt(key: String): Int? {
        sharedPreferences.getInt(key, -9999).let {
            if (it != -9999) {
                return it
            }
        }
        return null
    }
}