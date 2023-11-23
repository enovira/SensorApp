package com.yxh.sensor.core.retrofit

import com.alibaba.fastjson.JSONObject
import com.yxh.sensor.core.retrofit.bean.ReportPropertiesBean
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiServer {

    @POST("/report-property")
    fun reportProperties(@Body properties: ReportPropertiesBean): Call<JSONObject>

}