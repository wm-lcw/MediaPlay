package com.example.mediaplayproject.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MultiLanguageUtil;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;

import java.util.Locale;

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
        // 初始化打印
        boolean isOpenLog = (Boolean) SharedPreferencesUtil.getData(Constant.LOG_SWITCH,true);
        int logLevel = (Integer) SharedPreferencesUtil.getData(Constant.LOG_LEVEL,1);
        DebugLog.init(this, isOpenLog, logLevel);
        //声明Activity管理
        activityManager = new ActivityManager();
        context = getApplicationContext();
        application = this;

        // 注册Activity生命周期监听回调，此部分一定加上，因为有些版本不加的话多语言切换不回来
        registerActivityLifecycleCallbacks(callbacks);
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

    ActivityLifecycleCallbacks callbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            String language = (String) SharedPreferencesUtil.getData(Constant.CURRENT_LANGUAGE,"");
            String country = (String) SharedPreferencesUtil.getData(Constant.CURRENT_COUNTRY,"");
            if (!TextUtils.isEmpty(language) && !TextUtils.isEmpty(country)) {
                // 手动设置了语言（不跟随系统），且sp中和app中的多语言信息不相同时，才修改应用语言
                if (MultiLanguageUtil.isSameWithSetting(activity)) {
                    Locale locale = new Locale(language, country);
                    MultiLanguageUtil.changeAppLanguage(activity, locale, false);
//                    activity.recreate();
                }
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
        //Activity 其它生命周期的回调
    };

    @Override
    protected void attachBaseContext(Context base) {
        // attachBaseContext是在onCreate之前执行的，所以要将SharedPreferencesUtil的初始化放到这里执行
        SharedPreferencesUtil.getInstance(base,"mediaPlay");
        // 系统语言等设置发生改变时会调用此方法，需要要重置app语言
         super.attachBaseContext(MultiLanguageUtil.attachBaseContext(base));

        /// MultiLanguageUtil.attachBaseContext(base)的作用与前面onActivityCreated()方法的作用似乎有些重合
        /// 实测不调用MultiLanguageUtil.attachBaseContext(base) 也能正常执行
//        super.attachBaseContext(base);
    }
}

