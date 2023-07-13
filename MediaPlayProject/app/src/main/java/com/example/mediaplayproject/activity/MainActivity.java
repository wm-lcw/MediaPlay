package com.example.mediaplayproject.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.base.BasicActivity;
import com.example.mediaplayproject.utils.DebugLog;


/**
 * @author wm
 */
public class MainActivity extends BasicActivity {

    private static final int REQUEST_STORE_CODE = 1024;
    private static final int REQUEST_ALERT_CODE = 1025;
    private static final int REQUEST_ALL_CODE = 1026;
    private static final String TAG = "MainActivity";
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        requestPermission();
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.bt_music) {
                startActivity(new Intent(MainActivity.this, MusicPlayActivity.class));
            } else if (view.getId() == R.id.bt_video) {
                startActivity(new Intent(MainActivity.this, VideoActivity.class));
            }
        }
    };

    /**
     *  @version V1.0
     *  @Title requestPermission
     *  @author wm
     *  @createTime 2023/2/3 17:47
     *  @description 申请权限
     *  @param
     *  @return
     */
    private void requestPermission() {
        //主要是判断安卓11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                DebugLog.debug("store permission ok");
                if (hasAlertPermission()) {
                    DebugLog.debug("alert permission ok");
                    initData();
                } else {
                    DebugLog.debug("request alert permission");
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivityForResult(intent, REQUEST_ALERT_CODE);
                }
            } else {
                Toast.makeText(MainActivity.this, "请打开存储权限!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                startActivityForResult(intent, REQUEST_STORE_CODE);
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
     *  @version V1.0
     *  @Title hasAlertPermission
     *  @author wm
     *  @createTime 2023/2/3 17:44
     *  @description 判断是否有悬浮窗权限
     *  @param
     *  @return
     */
    private boolean hasAlertPermission() {
        AppOpsManager appOpsMgr = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        if (appOpsMgr == null) {
            return false;
        }
        int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), mContext.getPackageName());
        //这里只能为MODE_ALLOWED，若加上AppOpsManager.MODE_IGNORED会判断为真
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     *  @version V1.0
     *  @Title onRequestPermissionsResult
     *  @author wm
     *  @createTime 2023/2/3 17:49
     *  @description 这个方法主要针对SDK<=23的情况
     *  @param
     *  @return
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
     *  @version V1.0
     *  @Title onActivityResult
     *  @author wm
     *  @createTime 2023/2/3 17:50
     *  @description 这个方法主要针对安卓11以上
     *  @param
     *  @return
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_STORE_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                DebugLog.debug("request alert permission");
                Toast.makeText(MainActivity.this, "请打开悬浮窗权限!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivityForResult(intent, REQUEST_ALERT_CODE);
            } else {
                Toast.makeText(mContext, "store权限获取失败,程序即将退出", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == REQUEST_ALERT_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (hasAlertPermission()) {
                initData();
            } else {
                Toast.makeText(mContext, "alert权限获取失败,程序即将退出", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     *  @version V1.0
     *  @Title initData
     *  @author wm
     *  @createTime 2023/2/3 17:50
     *  @description 初始化按钮
     *  @param
     *  @return
     */
    private void initData() {
        Toast.makeText(mContext, "已获取存储权限", Toast.LENGTH_SHORT).show();
        findViewById(R.id.bt_music).setOnClickListener(mListener);
        findViewById(R.id.bt_video).setOnClickListener(mListener);

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }
}
