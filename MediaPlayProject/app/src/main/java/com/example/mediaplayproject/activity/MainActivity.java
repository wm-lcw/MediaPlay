package com.example.mediaplayproject.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.MusicViewPagerAdapter;
import com.example.mediaplayproject.base.BaseFragment;
import com.example.mediaplayproject.base.BasicActivity;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.fragment.DefaultListFragment;
import com.example.mediaplayproject.fragment.DiscoveryFragment;
import com.example.mediaplayproject.fragment.FavoriteListFragment;
import com.example.mediaplayproject.fragment.MainViewFragment;
import com.example.mediaplayproject.fragment.MusicPlayFragment;
import com.example.mediaplayproject.fragment.PersonalPageFragment;
import com.example.mediaplayproject.fragment.ToolsFragment;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicPlayerHelper;
import com.google.android.material.navigation.NavigationView;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wm
 */
public class MainActivity extends BasicActivity {

    private static final int REQUEST_ALL_CODE = 1026;
    private static boolean isFloatWindowPermissionRequested = false;
    private Context mContext;
    private ActivityResultLauncher<Intent> intentActivityResultLauncher;
    private MainViewFragment mainViewFragment;
    private MusicPlayFragment musicPlayFragment;

    /**
     * 手势滑动水平方向的最小距离
     */
    private static final int FLING_MIN_DISTANCE = 50;
    /**
     * 手势滑动垂直方向的最小距离
     */
    private static final int FLING_MIN_VELOCITY = 0;


    /**
     * 正在播放的列表
     */
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    /**
     * 默认的列表
     */
    private List<MediaFileBean> defaultList = new ArrayList<>();
    /**
     * 收藏的列表
     */
    private List<MediaFileBean> favoriteList = new ArrayList<>();
    /**
     * 当前播放歌曲的下标
     */
    private int mPosition = 0;
    /**
     * playMode:播放模式 0->循环播放; 1->随机播放; 2->单曲播放;
     * 主要是控制播放上下曲的position
     */
    private int playMode = 0;

    /**
     * musicListMode:播放的来源 0->默认列表; 1->收藏列表; 后面可以扩展其他的列表
     */
    private int musicListMode = 0;

    private boolean firstPlay = true, isInitPlayHelper = false;

    /**
     * MusicPlayService对象，控制音乐播放service类
     */
    private MusicPlayService musicService;
    /**
     * @createTime 2023/2/4 15:02
     * @description 创建ServiceConnection
     */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugLog.debug("onServiceConnected");
            musicService = ((MusicPlayService.MyBinder) service).getService(mContext);
            if (musicService != null) {
                // service创建成功的时候立即初始化
                initServicePlayHelper();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            DebugLog.debug("onServiceDisconnected");
        }
    };



    final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("ResourceAsColor")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_PLAY_ICON) {
                //service发送的信息，用于更新播放状态的图标
                boolean isPlaying = msg.getData().getBoolean("iconType");
//                ivMediaPlay.setImageResource(isPlaying ? R.mipmap.media_pause : R.mipmap.media_play);
                //接收到service发送的播放状态改变之后，刷新Activity的值（针对未刷新页面的情况）
                firstPlay = false;
                isInitPlayHelper = true;
                mPosition = musicService.getPosition();
            }
        }
    };


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        // 申请权限的结果回调处理
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

        // 申请权限
        requestPermission();


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解绑服务：注意bindService后 必须要解绑服务，否则会报-连接资源异常
        if (null != connection) {
            unbindService(connection);
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/3 17:47
     * @description 申请权限
     */
    private void requestPermission() {
        // 主要是判断安卓11
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
     * @description 这个方法主要针对SDK<= 23 的情况
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


    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        Toast.makeText(mContext, "已获取存储权限", Toast.LENGTH_SHORT).show();

        //初始化音乐列表资源
        DataRefreshService.initResource();
        mainViewFragment = MainViewFragment.getInstance(mContext);
        musicPlayFragment = MusicPlayFragment.getInstance(mContext);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fl_main_view, mainViewFragment);
        transaction.commit();

        // 从BasicApplication中获取音乐列表，上次播放的信息等
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        playMode = DataRefreshService.getLastPlayMode();
        musicListMode = DataRefreshService.getLastPlayListMode();
        mPosition = DataRefreshService.getLastPosition();

        switchMusicList();

        //启动MusicPlayService服务
        Intent bindIntent = new Intent(MainActivity.this, MusicPlayService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // 这里应该不用每次都调用，启动app时只调用一次即可，需要处理
        // 不然initServicePlayHelper刚设置service，下面又在service中获取，相当于是没更新
        initServicePlayHelper();
        // 重新进入界面之后都获取一下音量信息和当前音乐列表
        if (musicService != null) {
            //再次进入界面时刷新播放状态按钮，初次进入默认为暂停状态
//            ivPlayMusic.setImageResource(musicService.isPlaying() ? R.mipmap.media_pause : R.mipmap.media_play);
            //isInitPlayHelper：是否已经初始化; firstPlay :首次播放
            isInitPlayHelper = musicService.getInitResult();
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListMode = musicService.getMusicListMode();
        }

        mainViewFragment.setDataFromMainActivity(musicService,musicListMode,mPosition,myFragmentCallBack);
        musicPlayFragment.setDataFromMainActivity(musicService,musicListMode,mPosition);
        DebugLog.debug("listSize " + musicInfo.size() + "; music service "+ musicService);
    }



    /**
     * 改变当前的列表
     * @createTime 2023/8/20 10:35
     */
    private void switchMusicList() {
        if (musicListMode == 0) {
            musicInfo = defaultList;
        } else if (musicListMode == 1) {
            musicInfo = favoriteList;
        }
        //保存上次播放的列表来源
        DataRefreshService.setLastPlayListMode(musicListMode);
        //改变播放列表的时候，刷新播放器中的音乐列表来源
        initServicePlayHelper();
    }

    private void initServicePlayHelper() {
        if (musicService != null) {
            // 刷新Service里面的内容时，不用每次都初始化，最主要的是更新position和musicInfo
            // 初始化的时候需要先调用initPlayData方法更新各项数据，避免数组越界
            musicService.initPlayData(musicInfo, mPosition, musicListMode, playMode);
            if (!isInitPlayHelper) {
                isInitPlayHelper = true;
                musicService.initPlayHelper(handler);
            }
        }
    }


    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
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
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(
            1, new BasicThreadFactory.Builder().namingPattern("scheduled-pool-%d").daemon(true).build());

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

    public interface FragmentCallBack{
        /**
         *  切换Fragment
         *  @author wm
         *  @createTime 2023/8/23 18:31
         */
        void changeFragment();
    }

    public MyFragmentCallBack myFragmentCallBack = new MyFragmentCallBack();
    public class MyFragmentCallBack implements FragmentCallBack{

        @Override
        public void changeFragment() {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fl_main_view, musicPlayFragment);
            transaction.commit();
            musicPlayFragment.setDataFromMainActivity(musicService,musicListMode,mPosition);
        }
    }


}
