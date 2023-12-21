package com.example.mediaplayproject.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.LockScreenActivity;
import com.example.mediaplayproject.activity.MainActivity;
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

        // 调用startForeground() 方法
        // 创建一个通知，创建通知前记得获取开启通知权限
        startForeground(1, getNotification(this, "APP通知", "欢迎来到APP！"));

        // 不创建通知, 这种处理方式App起不来
//        startForeground(1, null);
    }

    /**
     *  创建锁屏服务的通知
     *  @author wm
     *  @createTime 2023/12/21 15:15
     * @param context: 
     * @param title: 
     * @param text: 
     * @return : android.app.Notification
     */
    private Notification getNotification(Context context, String title, String text) {
        // 是否静音
        boolean isSilent = true;
        // 是否持续(为不消失的常驻通知)
        boolean isOngoing = true;
        String channelName = "服务常驻通知";
        String channelId = "Service_Id";
        String category = Notification.CATEGORY_SERVICE;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent nfIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, nfIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                //设置PendingIntent
                .setContentIntent(pendingIntent)
                //设置状态栏内的小图标
                .setSmallIcon(R.mipmap.ic_launcher)
                //设置标题
                .setContentTitle(title)
                //设置内容
                .setContentText(text)
                //设置通知公开可见
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //设置持续(不消失的常驻通知)
                .setOngoing(isOngoing)
                //设置类别
                .setCategory(category)
                //优先级为：重要通知
                .setPriority(NotificationCompat.PRIORITY_MAX);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 安卓8.0以上系统要求通知设置Channel,否则会报错
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            // 锁屏显示通知
            notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(notificationChannel);
            builder.setChannelId(channelId);
        }
        return builder.build();
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
