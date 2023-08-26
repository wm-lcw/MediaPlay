package com.example.mediaplayproject.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.base.BasicActivity;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.fragment.MainViewFragment;
import com.example.mediaplayproject.fragment.MusicPlayFragment;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.ArrayList;
import java.util.List;

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
     * ServiceConnection对象
     */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugLog.debug("onServiceConnected");
            musicService = ((MusicPlayService.MyBinder) service).getService(mContext);
            if (musicService != null) {
                // service创建成功的时候立即初始化
                initServicePlayHelper();
                // 需要等service起来之后再给Fragment传service
                mainViewFragment.setDataFromMainActivity(musicService, myFragmentCallBack);
                musicPlayFragment.setDataFromMainActivity(musicService, musicListMode, mPosition);
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
                // service发送的信息，用于更新播放状态的图标
                boolean isPlaying = msg.getData().getBoolean("iconType");
                int newPosition = msg.getData().getInt("position");
                if (musicPlayFragment.isVisible()) {
                    musicPlayFragment.refreshPlayState(isPlaying, newPosition);
                }
                if (mainViewFragment.isVisible()) {
                    mainViewFragment.setPlayState(isPlaying);
                }

                firstPlay = false;
                isInitPlayHelper = true;
                mPosition = musicService.getPosition();
            } else if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_POSITION) {
                //service发送的信息，用于删除歌曲后自动播放下一曲或者在通知栏切歌后的mPosition
                int newPosition = msg.getData().getInt("newPosition");
                if (musicPlayFragment.isVisible()) {
                    musicPlayFragment.setPositionByServiceListChange(newPosition);
                }
            } else if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_FROM_PLAY_HELPER) {
                int seekbarProgress = msg.getData().getInt("seekbarProgress");
                String currentMusicInfo = msg.getData().getString("currentPlayingInfo");
                String currentPlayTime = msg.getData().getString("currentTime");
                String mediaTime = msg.getData().getString("mediaTime");
                if (musicPlayFragment.isVisible()) {
                    musicPlayFragment.setCurrentPlayInfo(seekbarProgress, currentMusicInfo, currentPlayTime, mediaTime);
                }
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
     * 申请权限
     *
     * @author wm
     * @createTime 2023/8/24 18:10
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
     * 判断是否有悬浮窗权限
     *
     * @return : boolean
     * @author wm
     * @createTime 2023/8/24 18:10
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
     * 判断是否有文件存储权限
     *
     * @return : boolean
     * @author wm
     * @createTime 2023/8/24 18:11
     */
    private boolean hasStorePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return false;
    }

    /**
     * 申请权限回调结果，主要针对API<23的情况
     *
     * @param requestCode:  请求码
     * @param permissions:  权限数组
     * @param grantResults: 请求结果
     * @author wm
     * @createTime 2023/8/24 18:11
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
     * 初始化操作(音乐资源、Ui布局)
     *
     * @author wm
     * @createTime 2023/8/24 18:08
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        // 初始化音乐列表资源
        DataRefreshService.initResource();

        // 创建Fragment实例，并加载显示MainViewFragment
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

        // 初始化播放的列表
        switchMusicList();

        // 启动MusicPlayService服务
        Intent bindIntent = new Intent(MainActivity.this, MusicPlayService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        DebugLog.debug("");
        // 获取一次音量信息和当前音乐列表
        if (musicService != null) {
            isInitPlayHelper = musicService.getInitResult();
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListMode = musicService.getMusicListMode();
        }
    }

    /**
     * 根据播放模式设定当前播放列表
     *
     * @author wm
     * @createTime 2023/8/24 18:14
     */
    private void switchMusicList() {
        if (musicListMode == 0) {
            musicInfo = defaultList;
        } else if (musicListMode == 1) {
            musicInfo = favoriteList;
        }
        // 保存上次播放的列表来源
        DataRefreshService.setLastPlayListMode(musicListMode);
    }

    /**
     *  初始化musicService
     *  @author wm
     *  @createTime 2023/8/24 18:18
     */
    private void initServicePlayHelper() {
        DebugLog.debug("");
        if (musicService != null) {
            DebugLog.debug("service ");
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

//    /**
//     * 菜单、返回键响应
//     */
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            //调用双击退出函数
//            exitBy2Click();
//        }
//        return false;
//    }
//
//    private static Boolean isExit = false;
//    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(
//            1, new BasicThreadFactory.Builder().namingPattern("scheduled-pool-%d").daemon(true).build());
//
//    /**
//     * 双击退出函数
//     */
//    private void exitBy2Click() {
//        if (!isExit) {
//            // 准备退出
//            isExit = true;
//            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
//            // 第一个参数为执行体，第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间。
//            scheduledExecutorService.scheduleAtFixedRate(() -> {
//                // 取消退出
//                isExit = false;
//            }, 2, 10, TimeUnit.SECONDS);
//        } else {
//            BasicApplication.getActivityManager().finishAll();
//        }
//    }

    public interface FragmentCallBack {
        /**
         * 切换Fragment
         * @author wm
         * @createTime 2023/8/23 18:31
         */
        void changeFragment();
    }

    public MyFragmentCallBack myFragmentCallBack = new MyFragmentCallBack();

    public class MyFragmentCallBack implements FragmentCallBack {
        @Override
        public void changeFragment() {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.addToBackStack(null);
            transaction.replace(R.id.fl_main_view, musicPlayFragment);
            transaction.commit();
            musicPlayFragment.setDataFromMainActivity(musicService, musicListMode, mPosition);
        }
    }


}
