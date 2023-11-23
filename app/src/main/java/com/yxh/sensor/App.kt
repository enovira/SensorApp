package com.yxh.sensor

import android.os.Environment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.blankj.utilcode.util.LogUtils
import com.yxh.sensor.core.base.BaseApp
import com.yxh.sensor.core.global.EventViewModelStore
import java.io.File

class App : ViewModelStoreOwner, BaseApp() {

    lateinit var eventViewModelStore: EventViewModelStore

    override fun onCreate() {
        super.onCreate()
        _instance = this
        eventViewModelStore = ViewModelProvider(this)[EventViewModelStore::class.java]
        val path = Environment.getExternalStorageDirectory().path + "/demo_log" + File.separator
        //  createOrExistsDir(new File(path+"log.txt"));
        //  createOrExistsDir(new File(path+"log.txt"));
        LogUtils.getConfig().isLogSwitch = true
        LogUtils.getConfig().isLog2FileSwitch = true
        LogUtils.getConfig().dir = path
    }

    companion object {
        private var _instance: App? = null
        val instance: App get() = _instance!!
    }

    override val viewModelStore: ViewModelStore
        get() = ViewModelStore()
}