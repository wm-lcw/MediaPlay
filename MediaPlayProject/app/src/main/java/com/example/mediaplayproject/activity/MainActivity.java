package com.example.mediaplayproject.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.base.BasicActivity;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.DebugLog;
import com.google.android.material.navigation.NavigationView;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wm
 */
public class MainActivity extends BasicActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnTouchListener {

    private static final int REQUEST_ALL_CODE = 1026;
    private static boolean isFloatWindowPermissionRequested = false;
    private Context mContext;
    private ActivityResultLauncher<Intent> intentActivityResultLauncher;

    /**
     * 手势滑动水平方向的最小距离
     */
    private static final int FLING_MIN_DISTANCE = 50;
    /**
     * 手势滑动垂直方向的最小距离
     */
    private static final int FLING_MIN_VELOCITY = 0;
    private DrawerLayout drawerLayout;
    private GestureDetector mGestureDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        findViewById(R.id.bt_music).setOnClickListener(mListener);
        findViewById(R.id.iv_setting).setOnClickListener(mListener);
        drawerLayout = findViewById(R.id.drawer_layout);
        mGestureDetector = new GestureDetector(this, myGestureListener);
        drawerLayout.setOnTouchListener(this);
        // 必须设置setLongClickable为true 否则监听不到手势
        drawerLayout.setLongClickable(true);

        intentActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    DebugLog.debug("resultCode" + result.getResultCode());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (hasStorePermission()) {
                            if (hasAlertPermission()) {
                                initData();
                            } else {
                                if (isFloatWindowPermissionRequested) {
                                    Toast.makeText(mContext, "缺少必要权限,程序即将退出", Toast.LENGTH_SHORT).show();
                                    BasicApplication.getActivityManager().finishAll();
                                }
                                DebugLog.debug("store permission ok,request alert permission");
                                Toast.makeText(MainActivity.this, "请打开悬浮窗权限!", Toast.LENGTH_LONG).show();
                                isFloatWindowPermissionRequested = true;
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                intentActivityResultLauncher.launch(intent);
                            }
                        } else {
                            Toast.makeText(mContext, "缺少必要权限,程序即将退出", Toast.LENGTH_SHORT).show();
                            BasicApplication.getActivityManager().finishAll();
                        }

                    }
                }
        );
        requestPermission();
    }

    private final View.OnClickListener mListener = view -> {
        if (view.getId() == R.id.bt_music) {
            if (hasStorePermission() && hasAlertPermission()) {
                startActivity(new Intent(MainActivity.this, MusicPlayActivity.class));
            } else {
                Toast.makeText(MainActivity.this, "缺少必要权限，请重新启动应用并赋予权限!", Toast.LENGTH_LONG).show();
            }

        } else if (view.getId() == R.id.iv_setting) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    };

    /**
     * @author wm
     * @createTime 2023/2/3 17:47
     * @description 申请权限
     */
    private void requestPermission() {
        //主要是判断安卓11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (hasStorePermission()) {
                DebugLog.debug("store permission ok");
                if (hasAlertPermission()) {
                    DebugLog.debug("alert permission ok");
                    initData();
                } else {
                    Toast.makeText(MainActivity.this, "请打开文件存储权限!", Toast.LENGTH_LONG).show();
                    DebugLog.debug("request alert permission");
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intentActivityResultLauncher.launch(intent);
                }
            } else {
                Toast.makeText(MainActivity.this, "请打开存储权限!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                intentActivityResultLauncher.launch(intent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW) == PackageManager.PERMISSION_GRANTED) {
                initData();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW}, REQUEST_ALL_CODE);
            }
        } else {
            initData();
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/3 17:44
     * @description 判断是否有悬浮窗权限
     */
    private boolean hasAlertPermission() {
        AppOpsManager appOpsMgr = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        if (appOpsMgr == null) {
            return false;
        }
        int mode = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            mode = appOpsMgr.unsafeCheckOpNoThrow("android:system_alert_window", android.os.Process.myUid(), mContext.getPackageName());
        }
        //这里只能为MODE_ALLOWED，若加上AppOpsManager.MODE_IGNORED会判断为真
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * @author wm
     * @createTime 2023/8/3 18:19
     * @description 判断是否有文件存储权限
     */
    private boolean hasStorePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return false;
    }

    /**
     * @author wm
     * @createTime 2023/2/3 17:49
     * @description 这个方法主要针对SDK<= 2 3 的情况
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ALL_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW) == PackageManager.PERMISSION_GRANTED) {
                initData();
            } else {
                Toast.makeText(mContext, "权限获取失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/3 17:50
     * @description 初始化按钮
     */
    private void initData() {
        Toast.makeText(mContext, "已获取存储权限", Toast.LENGTH_SHORT).show();

        //初始化音乐列表资源
        DataRefreshService.initResource();
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    /**
     * 菜单、返回键响应
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //调用双击退出函数
            exitBy2Click();
        }
        return false;
    }

    private static Boolean isExit = false;
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("scheduled-pool-%d").daemon(true).build());

    /**
     * 双击退出函数
     */
    private void exitBy2Click() {
        if (!isExit) {
            // 准备退出
            isExit = true;
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            // 第一个参数为执行体，第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间。
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                // 取消退出
                isExit = false;
            }, 2, 10, TimeUnit.SECONDS);
        } else {
            BasicApplication.getActivityManager().finishAll();
        }
    }



    /**
     * activity实现OnTouchListener接口，用来检测手势滑动
     * 这里直接调用GestureDetector.SimpleOnGestureListener的onTouchEvent方法来处理
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    /**
     * 创建一个GestureDetector.SimpleOnGestureListener对象，用来识别各种手势动作
     * 源码中SimpleOnGestureListener实现的是OnGestureListener, OnDoubleTapListener这两个接口，
     * 如果只是做检测左右滑动可以去只实现OnGestureListener，
     * 然后覆盖public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)方法即可
     */
    GestureDetector.SimpleOnGestureListener myGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            DebugLog.debug("开始滑动");
            float x = e1.getX() - e2.getX();
            float x2 = e2.getX() - e1.getX();
            if (x > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                drawerLayout.closeDrawer(GravityCompat.START);
                DebugLog.debug("show drawerLayout");

            } else if (x2 > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                drawerLayout.openDrawer(GravityCompat.START);
                DebugLog.debug("hide drawerLayout");
            }
            return false;
        }
    };


}
