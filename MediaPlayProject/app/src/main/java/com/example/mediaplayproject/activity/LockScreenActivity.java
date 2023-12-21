package com.example.mediaplayproject.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.view.LockScreenView;

/**
 * @author wm
 */
public class LockScreenActivity extends AppCompatActivity {

    private Context mContext;
    private LockScreenView mContentView;
    private LockBroadcastReceiver mLockBroadcastReceiver;


    private final ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // 锁屏界面所有子view测量完成后，获取decorView的宽度, 传给LockScreenView
            int mWindowWidth = getWindow().getDecorView().getWidth();
            mContentView.setWindowWidth(mWindowWidth);
            mContentView.setActivity(LockScreenActivity.this);
            if (mContentView != null) {
                mContentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 应用在前台才能启动，在后台无法启动，原因后面再分析
        mContext = this;
        initWindow();
        setContentView(R.layout.activity_lock_screen);
        mContentView = findViewById(R.id.lock_screen_view);
        mContentView.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.REFRESH_PLAY_STATE_ACTION);
        mLockBroadcastReceiver = new LockBroadcastReceiver();
        registerReceiver(mLockBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mContentView.initData();
    }

    /**
     * 初始化窗口
     *
     * @author wm
     * @createTime 2023/12/20 18:02
     */
    private void initWindow() {
        final Window window = getWindow();
        // 无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 取消系统锁屏
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                // 锁屏时仍显示
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 防止系统栏隐藏时activity大小发生变化
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN; // 全屏
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // 沉浸式
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // 隐藏导航栏
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION); // 隐藏导航栏

        window.setNavigationBarColor(Color.TRANSPARENT);
//        window.setStatusBarColor(Color.TRANSPARENT);

        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onBackPressed() {
        // 锁屏界面状态下屏蔽back键
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 屏蔽recent键
//        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
//        activityManager.moveTaskToFront(getTaskId(), 0);
    }

    class LockBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            DebugLog.debug("action " + action);
            if (Constant.REFRESH_PLAY_STATE_ACTION.equals(action)){
                mContentView.initData();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLockBroadcastReceiver != null){
            unregisterReceiver(mLockBroadcastReceiver);
        }
    }
}