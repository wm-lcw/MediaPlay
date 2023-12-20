package com.example.mediaplayproject.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.mediaplayproject.activity.LockScreenActivity;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;

/**
 * @author wm
 * @Classname LockScreenService
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/12/20 11:08
 * @Created by wm
 */
public class LockScreenService extends Service {

    private LockScreenReceiver mScreenReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenReceiver = new LockScreenReceiver();
        registerReceiver(mScreenReceiver, intentFilter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        DebugLog.debug("");
        return null;
    }

    static class LockScreenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 判断锁屏显示开关
            boolean lockScreenSwitch = (Boolean) SharedPreferencesUtil.getData(Constant.LOCK_SCREEN_SWITCH,false);
            if (!lockScreenSwitch){
                return;
            }
            String action = intent.getAction();
            DebugLog.debug("action " + action);
            if (Intent.ACTION_SCREEN_OFF.equals(action)){
                // 启动锁屏Activity
                Intent startIntent = new Intent(context, LockScreenActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                context.startActivity(startIntent);
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScreenReceiver != null){
            unregisterReceiver(mScreenReceiver);
        }
    }
}
