package com.yxh.sensor.core.global

// app启动预设值
object ConstantStore {
    //默认上传频率(次/180秒)
    const val defaultFrequency: Int = 180
    //最高上传频率(次/n秒)
    const val highestFrequency: Int = 120
    //默认服务器的ip地址
    const val defaultIp: String = "192.168.1.137"
    //默认服务器的端口号
    const val defaultPort: Int = 18891
}