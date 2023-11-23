package com.yxh.sensor.mvvm.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson.JSONObject
import com.google.gson.Gson
import com.yxh.sensor.core.global.SPKey
import com.yxh.sensor.core.retrofit.ApiServer
import com.yxh.sensor.core.retrofit.bean.CustomPosition
import com.yxh.sensor.core.retrofit.bean.CustomProperties
import com.yxh.sensor.core.retrofit.bean.ReportPropertiesBean
import com.yxh.sensor.core.utils.SPUtils
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WorkActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var apiServer: ApiServer? = null

    var heartRate: MutableLiveData<Int> = MutableLiveData(0)
    var bloodOxygen: MutableLiveData<Int> = MutableLiveData(0)
    var stepCount: MutableLiveData<Int> = MutableLiveData(0)
    private val imei by lazy {
        SPUtils.instance.getString(SPKey.key_imei)
    }

    private fun initApiServer() {
        if (apiServer == null) {
            val sb = StringBuilder()
            var flag = true
            SPUtils.instance.run {
                getString(SPKey.key_ip_address)?.let {
                    sb.append("http://").append(it)
                } ?: kotlin.run {
                    flag = false
                    Toast.makeText(getApplication(), "尚未配置平台ip", Toast.LENGTH_SHORT).show()
                }
                getInt(SPKey.key_port)?.let {
                    sb.append(":").append(it)
                } ?: kotlin.run {
                    flag = false
                    Toast.makeText(getApplication(), "尚未配置平台端口", Toast.LENGTH_SHORT).show()
                }
            }
            if (!flag) {
                return
            }
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor {
                    val request = it.request().newBuilder()
                        .addHeader("Content-Type", "application/json;charset=UTF-8").build()
                    it.proceed(request)
                }
                .build()
            val retrofit: Retrofit = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(sb.toString())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiServer = retrofit.create(ApiServer::class.java)
        }
    }

    fun reportProperties(customPosition: CustomPosition) {
        if (imei == null) {
            Toast.makeText(getApplication(), "imei为空", Toast.LENGTH_SHORT).show()
            return
        }
        initApiServer()
        imei?.let{
            val reportPropertiesBean = ReportPropertiesBean(
                it,
                CustomProperties(
                    heartRate.value,
                    bloodOxygen.value,
                    stepCount.value,
                    customPosition
                )
            )
            println(Gson().toJson(reportPropertiesBean))
            val call = apiServer?.reportProperties(reportPropertiesBean)
            call?.enqueue(object : Callback<JSONObject> {
                override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                    println("请求成功")
                    println(response.body()?.toJSONString())
                }

                override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                    println("请求失败")
                    t.printStackTrace()
                }
            })
        }
    }
}