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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.viewpager.ListViewPagerAdapter;
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
    private ListViewPagerAdapter listViewPagerAdapter;
    private ViewPager2 musicListViewPager;

    private boolean isPlaying = false, firstPlay = true;
    private int mPosition = 0;
    private int playMode = Constant.PLAY_MODE_LOOP;
    private String musicListName = Constant.LIST_MODE_DEFAULT_NAME;
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private List<MediaFileBean> defaultList = new ArrayList<>();
    private List<MediaFileBean> favoriteList = new ArrayList<>();
    private List<MediaFileBean> historyList = new ArrayList<>();

    private LinearLayout mFloatLayout;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private ArrayList<PlayListFragment> viewPagerLists;
    private boolean isShowList = false, mRegistered = false;
    private MainMusicBroadcastReceiver mainMusicBroadcastReceiver;

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
                musicPlayFragment.setDataFromMainActivity(musicService, handler, musicListName, mPosition);
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
        // 让状态栏保持可见
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
            // 在MainActivity失去焦点时，悬浮窗关闭, 需要判断mFloatLayout正在显示才执行移除，否则会报错
            mWindowManager.removeView(mFloatLayout);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DebugLog.debug("");
        // 解绑服务
        if (null != connection) {
            unbindService(connection);
        }
        unregisterReceiver();
        // 需要添加下面的强制退出逻辑；否则在主页点返回退出app时，关闭不彻底，导致某些资源（ViewPager、MediaPlayer等）未能释放，重启app会闪退报错
        System.exit(0);
    }

    /**
     * 申请权限
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
     * @author wm
     * @createTime 2023/8/24 18:08
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        // 初始化音乐列表资源
        DataRefreshService.initResource();

        // 从DataRefreshService中获取音乐列表，上次播放的信息等
        playMode = DataRefreshService.getLastPlayMode();
        musicListName = DataRefreshService.getLastPlayListName();
        mPosition = DataRefreshService.getLastPosition();
        musicInfo = DataRefreshService.getMusicListByName(musicListName);
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        historyList = DataRefreshService.getHistoryList();

        // 创建悬浮窗视图
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

        registerReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DebugLog.debug("");
        // 从service获取音量信息和当前音乐列表，应对息屏唤醒等的情况
        if (musicService != null) {
            isPlaying = musicService.isPlaying();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListName = musicService.getMusicListName();
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
        if (mainViewFragment.isVisible()){
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                //调用双击退出函数
                exitBy2Click();
            }
        } else if (musicPlayFragment.isVisible()){
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fl_main_view, mainViewFragment);
            transaction.commit();
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
            Toast.makeText(this, "再按一次回到主页", Toast.LENGTH_SHORT).show();
            // 第一个参数为执行体，第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间。
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                // 取消退出
                isExit = false;
            }, 2, 10, TimeUnit.SECONDS);
        } else {
            moveTaskToBack(false);
        }
    }

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
            // 切换Fragment，后续拓展多个Fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.addToBackStack(null);
            transaction.replace(R.id.fl_main_view, musicPlayFragment);
            transaction.commit();
            musicPlayFragment.setDataFromMainActivity(musicService, handler, musicListName, mPosition);
        }
    }

    /**
     *  处理各种Handler消息
     *  @createTime 2023/9/3 15:44
     */
    final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("ResourceAsColor")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_PLAY_STATE) {
                // service发送的信息，更新播放状态
                isPlaying = msg.getData().getBoolean("isPlayingStatus");
                mPosition = msg.getData().getInt("position");
                firstPlay = musicService.getFirstPlay();
                refreshFragmentStatus();
                refreshListStatus();
            } else if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_LIST_STATE) {
                // MusicPlayFragment发送的消息，播放页面收藏/取消收藏时刷新列表
                refreshFragmentStatus();
                refreshListStatus();
            } else if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_FROM_PLAY_HELPER) {
                // playHelper发送的消息，用于更新播放进度，目前只用于MusicPlayFragment
                int seekbarProgress = msg.getData().getInt("seekbarProgress");
                String currentMusicInfo = msg.getData().getString("currentPlayingInfo");
                String currentPlayTime = msg.getData().getString("currentTime");
                String mediaTime = msg.getData().getString("mediaTime");
                if (musicPlayFragment.isVisible()) {
                    musicPlayFragment.refreshCurrentPlayInfo(seekbarProgress, currentMusicInfo, currentPlayTime, mediaTime);
                }
            } else if (msg.what == Constant.HANDLER_MESSAGE_SHOW_LIST_FRAGMENT) {
                // 显示音乐列表悬浮窗
                showFloatView();
            }
        }
    };



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
        wmParams.gravity = Gravity.START | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        // 设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        initFloatView();
    }

    /**
     *  初始化悬浮窗中的视图、初始化Fragment等
     *  @author wm
     *  @createTime 2023/9/3 16:50
     */
    @SuppressLint("InflateParams")
    private void initFloatView() {
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        // 获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.layout_list_view_pager, null);
        setWindowOutTouch();
        musicListViewPager = mFloatLayout.findViewById(R.id.list_view_pager);
        PlayListFragment defaultListFragment = new PlayListFragment(mContext, musicInfo, Constant.LIST_MODE_DEFAULT_NAME, Constant.LIST_SHOW_MODE_CURRENT);
        PlayListFragment favoriteListFragment = new PlayListFragment(mContext, favoriteList, Constant.LIST_MODE_FAVORITE_NAME, Constant.LIST_SHOW_MODE_FAVORITE);
        PlayListFragment historyListFragment = new PlayListFragment(mContext, historyList, Constant.LIST_MODE_HISTORY_NAME, Constant.LIST_SHOW_MODE_HISTORY);
        viewPagerLists = new ArrayList<>();
        viewPagerLists.add(defaultListFragment);
        viewPagerLists.add(favoriteListFragment);
        viewPagerLists.add(historyListFragment);
        listViewPagerAdapter = new ListViewPagerAdapter((FragmentActivity) mContext, viewPagerLists);
        musicListViewPager.setAdapter(listViewPagerAdapter);
    }

    /**
     * 显示悬浮窗
     * @author wm
     * @createTime 2023/9/2 14:18
     */
    private void showFloatView() {
        // 每次打开悬浮窗列表时都先同步一次数据
        if (viewPagerLists.get(0).isInitSuccess()){
            // 规定第一个FragmentList就是动态更改的，所以直接用get(0)获取第一个页面
            viewPagerLists.get(0).changePlayList(musicInfo,musicListName,mPosition);
        }
        // 每次打开默认选中第一个列表，即当前播放列表
        musicListViewPager.setCurrentItem(0);
        mWindowManager.addView(mFloatLayout, wmParams);
        isShowList = true;
        refreshListStatus();
    }

    /**
     *  点击窗口外部区域关闭列表窗口
     *  @author wm
     *  @createTime 2023/9/3 16:51
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

    /**
     *  刷新悬浮窗列表的状态
     *  @author wm
     *  @createTime 2023/9/3 15:36
     */
    private void refreshListStatus() {
        for (PlayListFragment fragment : viewPagerLists) {
            if (musicListName.equalsIgnoreCase(fragment.getListName())) {
                fragment.setSelectPosition(mPosition);
                fragment.setSelection(mPosition);
            } else {
                fragment.setSelectPosition(-1);
            }
        }
    }

    /**
     *  刷新Fragment的数据和UI状态
     *  @author wm
     *  @createTime 2023/9/3 15:36
     */
    private void refreshFragmentStatus(){
        if (musicPlayFragment.isVisible()) {
            musicPlayFragment.refreshPlayState(isPlaying, mPosition, musicListName, musicInfo, firstPlay);
        }
        if (mainViewFragment.isVisible()) {
            mainViewFragment.refreshPlayState(isPlaying, mPosition, musicListName, musicInfo, firstPlay);
        }
        // 在mainViewFragment不可见的情况下也需要刷新PersonalPageFragment，所以不能放到上面的isVisible()中
        mainViewFragment.refreshCustomerList();
    }

    /**
     *  删除音乐的广播
     *  @author wm
     *  @createTime 2023/9/3 15:38
     */
    private class MainMusicBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Constant.CHANGE_MUSIC_ACTION.equals(intent.getAction())){
                // 列表Fragment或service发送的消息，用于通知切换播放歌曲/播放列表
                int newPosition = intent.getExtras().getInt("position");
                String newMusicListName = intent.getExtras().getString("musicListName");
                changeMusicOrList(newPosition, newMusicListName);
            } else if(Constant.OPERATE_CUSTOMER_MUSIC_LIST_ACTION.equals(intent.getAction())){
                // DataRefreshService发送的广播，用于通知自定义列表中有新加列表或删除列表的操作
                int operation = intent.getExtras().getInt("listOperation");
                String listName = intent.getExtras().getString("listName");
                if (operation == Constant.CUSTOMER_LIST_OPERATOR_DELETE && musicListName.equalsIgnoreCase(listName)){
                    // 删除列表的操作，且是当前正在播放的列表，需要做停止播放的操作
                    stopPlayByDelete();
                }
                // 更新各个Fragment的数据
                refreshFragmentStatus();
                // 更新列表Ui
                refreshListStatus();
            } else if(Constant.STOP_PLAY_CUSTOMER_MUSIC_ACTION.equals(intent.getAction())){
                // DataRefreshService发送的广播，删除的歌曲列表里含有当前播放歌曲的情况下发送的停止播放歌曲广播
                // 这个广播发送时一定会跟着发送OPERATE_MUSIC_ACTION
                // 所以这里只需要停止播放，不需要更新UI，更新UI的操作在OPERATE_MUSIC_ACTION广播里面处理
                musicService.toStop();
                mPosition = 0;
                isPlaying = false;
                firstPlay = true;
            } else if(Constant.OPERATE_MUSIC_ACTION.equals(intent.getAction())){
                // DataRefreshService发送的歌曲增删操作的广播
                String listName = intent.getExtras().getString("listName");
                boolean deletePlayingMusic = intent.getExtras().getBoolean("deletePlayingMusic");
                if (musicListName.equalsIgnoreCase(listName)){
                    // 刷新列表
                    musicInfo = DataRefreshService.getMusicListByName(musicListName);
                    if (musicInfo.size() <= 0){
                        // 列表已清空，停止播放并切换到默认列表
                        stopPlayByDelete();
                    } else {
                        if (deletePlayingMusic){
                            // 当前播放的歌曲被删除，直接播放刷新列表后的第一首音乐
                            mPosition = 0;
                            musicService.play(musicInfo.get(0),true,0);
                        } else {
                            // 当前播放歌曲未被删除，或者是插入歌曲的情况，需要更新position
                            long lastMusicId = DataRefreshService.getLastMusicId();
                            for (int i = 0; i < musicInfo.size() ;i++){
                                if (lastMusicId == musicInfo.get(i).getId()){
                                    mPosition = i;
                                    break;
                                }
                            }
                        }

                    }
                }
                // 刷新service的播放列表信息
                musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);

                // 刷新播放列表的高亮状态
                for (PlayListFragment fragment : viewPagerLists) {
                    if (musicListName.equalsIgnoreCase(fragment.getListName())) {
                        fragment.setSelectPosition(mPosition);
                        fragment.setSelection(mPosition);
                    } else {
                        fragment.setSelectPosition(-1);
                    }
                }

                // 更新各个Fragment的数据
                refreshFragmentStatus();
                // 更新列表Ui
                refreshListStatus();
            }
        }
    }

    /**
     *  切换歌曲或播放列表
     *  @author wm
     *  @createTime 2023/9/3 15:45
     *  @param position: 音乐下标
     *  @param newMusicListName: 音乐列表名
     */
    private void changeMusicOrList(int position, String newMusicListName) {
        try {
            mPosition = position;
            DebugLog.debug("newListName " + newMusicListName + "; position " + position);

            if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(newMusicListName)) {
                // 如果是最近播放列表，需要保存播放信息，然后等待刷新列表的数据，拿到最新的列表再去播放和刷新UI高亮
                DataRefreshService.setLastPlayInfo(Constant.LIST_MODE_HISTORY_NAME, mPosition, historyList.get(mPosition).getId(), playMode);

                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 获取音乐列表，上次播放的信息等
                playMode = DataRefreshService.getLastPlayMode();
                mPosition = DataRefreshService.getLastPosition();
                historyList = DataRefreshService.getHistoryList();
                musicListName = Constant.LIST_MODE_HISTORY_NAME;
                musicInfo = historyList;
                if (viewPagerLists.get(0).isInitSuccess()) {
                    viewPagerLists.get(0).changePlayList(musicInfo, musicListName, mPosition);
                }
            } else {
                if (!musicListName.equalsIgnoreCase(newMusicListName)) {
                    // 有切换播放列表的操作才更新当前播放列表的信息
                    List<MediaFileBean> tempList = DataRefreshService.getMusicListByName(newMusicListName);
                    if (tempList != null && tempList.size() > 0) {
                        // 更新播放列表等数据
                        musicInfo = tempList;
                        musicListName = newMusicListName;
                        if (viewPagerLists.get(0).isInitSuccess()) {
                            // 这里要判断PlayListFragment是否已经初始化，否则打开app后首次点击主页的列表播放会报错
                            // 规定第一个FragmentList就是动态更改的，所以直接用get(0)获取第一个页面
                            viewPagerLists.get(0).changePlayList(musicInfo, musicListName, mPosition);
                        }
                    }
                }

                // 保存播放信息
                DataRefreshService.setLastPlayInfo(musicListName, mPosition, musicInfo.get(mPosition).getId(), playMode);
            }

            // 调用service播放
            if (musicService != null) {
                musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);
                musicService.play(musicInfo.get(mPosition), true, mPosition);
            }

        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }

        // 这里不需要手动去更新Fragment和列表的状态，service的play方法里面有发送Handler给Activity更新播放状态
    }

    /**
     *  删除歌曲后列表为空的操作
     *  @author wm
     *  @createTime 2023/9/3 15:39
     */
    private void stopPlayByDelete() {
        DebugLog.debug("");
        musicService.toStop();
        mPosition = 0;
        isPlaying = false;
        firstPlay = true;
        musicInfo = defaultList;
        musicListName = Constant.LIST_MODE_DEFAULT_NAME;

        // 直接保存默认列表的第一首歌作为最后的播放，避免此时关闭应用，导致下次打开时无法获取上次播放的信息
        DataRefreshService.setLastPlayInfo(musicListName,mPosition,musicInfo.get(mPosition).getId(),playMode);
    }

    /**
     *  注册广播
     *  @author wm
     *  @createTime 2023/9/3 15:39
     */
    public void registerReceiver() {
        mainMusicBroadcastReceiver = new MainMusicBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.CHANGE_MUSIC_ACTION);
        filter.addAction(Constant.OPERATE_CUSTOMER_MUSIC_LIST_ACTION);
        filter.addAction(Constant.OPERATE_MUSIC_ACTION);
        filter.addAction(Constant.STOP_PLAY_CUSTOMER_MUSIC_ACTION);
        mContext.registerReceiver(mainMusicBroadcastReceiver, filter);
        mRegistered = true;
    }

    /**
     *  注销广播
     *  @author wm
     *  @createTime 2023/9/3 15:40
     */
    public void unregisterReceiver() {
        if (mRegistered) {
            try {
                mContext.unregisterReceiver(mainMusicBroadcastReceiver);
                mainMusicBroadcastReceiver = null;
                mRegistered = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
