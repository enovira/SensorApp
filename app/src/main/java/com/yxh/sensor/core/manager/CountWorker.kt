package com.yxh.sensor.core.manager

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.blankj.utilcode.util.LogUtils
import java.util.concurrent.TimeUnit


internal class CountWorker(context: Context, workerParams: WorkerParameters)
    : Worker(context, workerParams){
    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiresStorageNotLow(false)
                .build()
            println("构建约束属性")
            val request =
//                OneTimeWorkRequestBuilder<CountWorker>()
                PeriodicWorkRequest.Builder(CountWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(Data.Builder().putString("parameter1", "value of parameter1").build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()
            println("建立周期性任务")
            WorkManager.getInstance(context).enqueue(request)
            println("开始执行")
//            val request = OneTimeWorkRequestBuilder<CountWorker>()
//                .setConstraints(constraints)
//                .setInputData(Data.Builder().putString("parameter1", "value of parameter1").build())
//                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
//                .build()
//            WorkManager.getInstance(context).enqueue(request)
//            val request = OneTimeWorkRequestBuilder<CountWorker>()
//                //-----1-----添加约束
//                .setConstraints(constraints)
//                //-----2----- 传入执行worker需要的数据
//                .setInputData(Data.Builder().putString("parameter1", "value of parameter1").build())
//                //-----3-----设置避退策略
//                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
//                .build()
//            //-----4-----将任务添加到队列中
//            //WorkManager.getInstance(context).enqueue(request)
//            //或者采用uniqueName执行
//            WorkManager.getInstance(context)
//                .beginUniqueWork("uniqueName", ExistingWorkPolicy.REPLACE, request).enqueue()
//            //-----5-----对任务加入监听
//            WorkManager.getInstance(context).getWorkInfoByIdLiveData(request.id)
//                .observe(context, Observer {
//                    //-----8----获取doWork中传入的参数
//                    LogUtils.d("workInfo ${it.outputData.getString("result1")} ${it.state}: ")
//                })
//            //或者采用tag的方式监听状态
//            WorkManager.getInstance(context).getWorkInfosByTagLiveData("tagCountWorker")
//                .observe(context, Observer {
////                    Log.i(
////                        "aaa",
////                        "workInfo tag-- ${it[0].outputData.getString("result1")} ${it[0].state}: "
////                    )
//                    Log.i("aaa", "workInfo tag-- ${it.size} ")
//                })
//            //或者采用uniqueName的形式监听任务执行的状态
//            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("uniqueName")
//                .observe(context, Observer {
//                    Log.i(
//                        "aaa",
//                        "workInfo uniqueName-- ${it[0].outputData.getString("result1")} ${it[0].state}: "
//                    )
//                })
        }
    }

    override fun doWork(): Result {
        for (i in 1..3) {
            Thread.sleep(500)
            println("hahahahahahahah")
            //-----6-----获取传入的参数
            Log.i("aaa", "count: $i parameter: ${inputData.getString("parameter1")}")
        }
        //-----7-----传入返回的参数
        return Result.success(Data.Builder().putString("result1", "value of result1").build())
    }
}