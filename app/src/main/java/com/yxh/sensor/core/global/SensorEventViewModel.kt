package com.yxh.sensor.core.global

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alibaba.fastjson.JSONObject
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
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
import java.security.InvalidParameterException

class SensorEventViewModel: ViewModel() {

    private var apiServer: ApiServer? = null

    /** 心率 */
    var heartRate: MutableLiveData<Int> = MutableLiveData(0)
    /** 血氧 */
    var bloodOxygen: MutableLiveData<Int> = MutableLiveData(0)
    /** 步数 */
    var stepCount: MutableLiveData<Int> = MutableLiveData(0)
    /** 计时 */
    val timeCountLiveData = MutableLiveData<Int>(0)

    val apiServerException = MutableLiveData<String?>()

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
                }
                getInt(SPKey.key_port)?.let {
                    sb.append(":").append(it)
                } ?: kotlin.run {
                    flag = false
                }
            }
            if (!flag) {
                throw InvalidParameterException("ip地址出错")
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

    fun reportProperties(customPosition: CustomPosition?) {
        if (imei == null) {
            throw InvalidParameterException("imei为空")
        }
        initApiServer()
        imei?.let{ it ->
            val reportPropertiesBean = ReportPropertiesBean(
                it,
                CustomProperties(
                    heartRate.value,
                    bloodOxygen.value,
                    stepCount.value,
                ).apply {
                    customPosition?.let { customPositionBean ->
                        position = customPositionBean
                    }
                }
            )
            LogUtils.d(Gson().toJson(reportPropertiesBean))
            val call = apiServer?.reportProperties(reportPropertiesBean)
            call?.enqueue(object : Callback<JSONObject> {
                override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                    LogUtils.d("请求成功")
//                    LogUtils.d(response.body()?.toJSONString())
                }

                override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                    LogUtils.d("请求失败")
                    LogUtils.e(t.cause)
                    t.printStackTrace()
                }
            })
        }
    }
}