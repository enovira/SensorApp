package com.yxh.sensor.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blankj.utilcode.util.LogUtils;

public class AlarmTaskReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("alarmBroadcast")) {
            LogUtils.d("接收定时闹钟广播，开始发送广播");
            context.sendBroadcast(new Intent("sensorWakeupBroadcast"));
        }
    }
}