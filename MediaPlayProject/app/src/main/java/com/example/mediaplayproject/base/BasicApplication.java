package com.example.mediaplayproject.base;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import com.example.mediaplayproject.utils.SharedPreferencesUtil;

/**
 * @author wm
 * @Description 工程管理
 * @Date 2023/2/9 10:59
 * @Created by wm
 */
public class BasicApplication extends Application {
    private static ActivityManager activityManager;
    @SuppressLint("StaticFieldLeak")
    private static BasicApplication application;
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        //声明Activity管理
        activityManager = new ActivityManager();
        context = getApplicationContext();
        application = this;
        SharedPreferencesUtil.getInstance(getContext(),"mediaPlay");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static ActivityManager getActivityManager() {
        return activityManager;
    }

    public static Context getContext() {
        return context;
    }

    public static BasicApplication getApplication() {
        return application;
    }
}

