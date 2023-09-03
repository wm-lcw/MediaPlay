package com.example.mediaplayproject.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
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
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.ListViewPagerAdapter;
import com.example.mediaplayproject.base.BasicActivity;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.fragment.MainViewFragment;
import com.example.mediaplayproject.fragment.MusicPlayFragment;
import com.example.mediaplayproject.fragment.PlayListFragment;
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

    private boolean isPlaying = false;
    private int mPosition = 0;
    private int playMode = Constant.PLAY_MODE_LOOP;
    private String musicListName = Constant.LIST_MODE_DEFAULT_NAME;
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private List<MediaFileBean> favoriteList = new ArrayList<>();

    private LinearLayout mFloatLayout;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private ArrayList<PlayListFragment> viewPagerLists;
    private boolean isShowList = false;


    private MusicPlayService musicService;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugLog.debug("onServiceConnected");
            musicService = ((MusicPlayService.MyBinder) service).getService(mContext);
            if (musicService != null) {
                // service创建成功的时候立即初始化
                musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);
                musicService.initPlayHelper(handler);
                // 需要等service起来之后再给Fragment传service
                mainViewFragment.setDataFromMainActivity(musicService, handler, myFragmentCallBack);
                musicPlayFragment.setDataFromMainActivity(musicService, handler, viewPagerLists, musicListName, mPosition);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            DebugLog.debug("onServiceDisconnected");
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugLog.debug("");
        mContext = this;

        // 申请权限的结果回调处理
        intentActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (hasStorePermission()) {
                            if (hasAlertPermission()) {
                                initData();
                            } else {
                                if (isFloatWindowPermissionRequested) {
                                    Toast.makeText(mContext, "缺少必要权限,程序即将退出", Toast.LENGTH_SHORT).show();
                                    BasicApplication.getActivityManager().finishAll();
                                }
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

    @Override
    protected void onPause() {
        super.onPause();
        DebugLog.debug("");
        if (mWindowManager != null && mFloatLayout.isAttachedToWindow()) {
            // 在MainActivity失去焦点时，悬浮窗关闭
            // 这里需要判断mFloatLayout正在显示才执行移除，否则会报错
            mWindowManager.removeView(mFloatLayout);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DebugLog.debug("");
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
                if (hasAlertPermission()) {
                    initData();
                } else {
                    Toast.makeText(MainActivity.this, "请打开文件存储权限!", Toast.LENGTH_LONG).show();
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

        // 从BasicApplication中获取音乐列表，上次播放的信息等
        playMode = DataRefreshService.getLastPlayMode();
        musicListName = DataRefreshService.getLastPlayListName();
        mPosition = DataRefreshService.getLastPosition();
        musicInfo = DataRefreshService.getMusicListByName(musicListName);
        favoriteList = DataRefreshService.getFavoriteList();

        createFloatView();

        // 创建Fragment实例，并加载显示MainViewFragment
        mainViewFragment = MainViewFragment.getInstance(mContext);
        musicPlayFragment = MusicPlayFragment.getInstance(mContext);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fl_main_view, mainViewFragment);
        transaction.commit();

        // 启动MusicPlayService服务
        Intent bindIntent = new Intent(MainActivity.this, MusicPlayService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        DebugLog.debug("");
        // 从service获取音量信息和当前音乐列表，应对息屏唤醒等的情况
        if (musicService != null) {
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListName = musicService.getMusicListName();
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
         *
         * @author wm
         * @createTime 2023/8/23 18:31
         */
        void changeFragment();
    }

    public MyFragmentCallBack myFragmentCallBack = new MyFragmentCallBack();

    public class MyFragmentCallBack implements FragmentCallBack {
        @Override
        public void changeFragment() {
            // 切换Fragment，后续拓展，多个Fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.addToBackStack(null);
            transaction.replace(R.id.fl_main_view, musicPlayFragment);
            transaction.commit();
            musicPlayFragment.setDataFromMainActivity(musicService, handler, viewPagerLists, musicListName, mPosition);
        }
    }

    final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("ResourceAsColor")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_PLAY_ICON) {
                // service发送的信息，更新播放状态的图标
                isPlaying = msg.getData().getBoolean("isPlayingStatus");
                mPosition = msg.getData().getInt("position");
                if (musicPlayFragment.isVisible()) {
                    musicPlayFragment.refreshPlayState(isPlaying, mPosition, musicListName, musicInfo);
                }
                if (mainViewFragment.isVisible()) {
                    mainViewFragment.refreshPlayState(isPlaying, mPosition, musicListName, musicInfo);
                }
            } else if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_POSITION) {
                // service删除歌曲后自动播放下一曲、在通知栏切歌后更新mPosition发送的信息，
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
            } else if (msg.what == Constant.HANDLER_MESSAGE_FROM_LIST_FRAGMENT) {
                int newPosition = msg.getData().getInt("position");
                String newMusicListName = msg.getData().getString("musicListName");
                List<MediaFileBean> tempList = DataRefreshService.getMusicListByName(musicListName);
                if (tempList != null && tempList.size() > 0) {
                    // 更新播放列表等数据
                    changeMusicPlayList(tempList, newPosition, newMusicListName);
                }
            } else if (msg.what == Constant.HANDLER_MESSAGE_SHOW_LIST_FRAGMENT) {
                DebugLog.debug("show list");
                showFloatView();
            }
        }
    };

    private void changeMusicPlayList(List<MediaFileBean> list, int position, String newMusicListName) {
        // 更新Activity中的数据
        musicInfo = list;
        mPosition = position;
        musicListName = newMusicListName;
        isPlaying = true;

        // 保存上次播放的列表来源
        DataRefreshService.setLastPlayListName(musicListName);
        DataRefreshService.setLastPosition(mPosition);
        DataRefreshService.setLastMusicId(musicInfo.get(mPosition).getId());

        // 更新各个Fragment的数据
        if (musicPlayFragment.isVisible()) {
            musicPlayFragment.refreshPlayState(isPlaying, mPosition, musicListName, musicInfo);
        }
        if (mainViewFragment.isVisible()) {
            mainViewFragment.refreshPlayState(isPlaying, mPosition, musicListName, musicInfo);
        }

        // 调用service播放
        if (musicService != null) {
            musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);
            musicService.play(musicInfo.get(mPosition), true, mPosition);
        }
    }

    /**
     *  创建悬浮窗
     *  @author wm
     *  @createTime 2023/9/3 11:48
     */
    private void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        // 获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        // 设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        // 设置背景为透明，否则滑动ListView会出现残影
        wmParams.format = PixelFormat.TRANSPARENT;
        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口,不设置这个flag的话，home页的划屏会有问题
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        // 调整悬浮窗显示的停靠位置为左侧顶部
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        // 设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        initFloatView();
    }

    /**
     * 初始化悬浮窗中的视图、初始化Fragment等
     */
    private void initFloatView() {
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.layout_list_view_pager, null);
        setWindowOutTouch();
        ViewPager2 musicListViewPager = mFloatLayout.findViewById(R.id.list_view_pager);
        PlayListFragment defaultListFragment = new PlayListFragment(mContext, musicInfo, Constant.LIST_MODE_DEFAULT_NAME, handler);
        PlayListFragment favoriteListFragment = new PlayListFragment(mContext, favoriteList, Constant.LIST_MODE_FAVORITE_NAME, handler);
        viewPagerLists = new ArrayList<>();
        viewPagerLists.add(defaultListFragment);
        viewPagerLists.add(favoriteListFragment);
        ListViewPagerAdapter listViewPagerAdapter = new ListViewPagerAdapter((FragmentActivity) mContext, viewPagerLists);
        musicListViewPager.setAdapter(listViewPagerAdapter);
    }

    /**
     * 显示悬浮窗
     *
     * @author wm
     * @createTime 2023/9/2 14:18
     */
    private void showFloatView() {
        // 添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
        isShowList = true;
    }

    /**
     * 点击窗口外部区域关闭列表窗口
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setWindowOutTouch() {
        /* 点击窗口外部区域可消除
         将悬浮窗设置为全屏大小，外层有个透明背景，中间一部分视为内容区域,
         所以点击内容区域外部视为点击悬浮窗外部
         其中popupWindowView为全屏，listWindow为列表区域，触摸点没有落在列表区域，则隐藏列表*/
        final View popupWindowView = mFloatLayout.findViewById(R.id.ll_popup_window);
        final View listWindow = mFloatLayout.findViewById(R.id.ll_listWindow);
        popupWindowView.setOnTouchListener((v, event) -> {
            DebugLog.debug("---");
            int x = (int) event.getX();
            int y = (int) event.getY();
            Rect rect = new Rect();
            listWindow.getGlobalVisibleRect(rect);
            if (!rect.contains(x, y) && isShowList) {
                mWindowManager.removeView(mFloatLayout);
                isShowList = false;
            }
            return true;
        });
    }
}
