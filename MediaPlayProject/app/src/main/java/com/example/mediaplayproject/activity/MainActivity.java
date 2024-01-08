package com.example.mediaplayproject.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.viewpager.ListViewPagerAdapter;
import com.example.mediaplayproject.base.BasicActivity;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.fragment.MainViewFragment;
import com.example.mediaplayproject.fragment.MusicPlayFragment;
import com.example.mediaplayproject.fragment.PlayListFragment;
import com.example.mediaplayproject.fragment.SettingsFragment;
import com.example.mediaplayproject.fragment.SplashFragment;
import com.example.mediaplayproject.fragment.tools.AboutFragment;
import com.example.mediaplayproject.fragment.tools.ChangeLanguageFragment;
import com.example.mediaplayproject.fragment.tools.StatisticsFragment;
import com.example.mediaplayproject.fragment.tools.TimingOffFragment;
import com.example.mediaplayproject.fragment.tools.WoodenFishFragment;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.StatusBar;
import com.example.mediaplayproject.utils.ToolsUtils;

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

    private ActivityResultLauncher<Intent> openFileManagerIntent;
    private MainViewFragment mainViewFragment;
    private MusicPlayFragment musicPlayFragment;
    private StatisticsFragment statisticsFragment;
    private ChangeLanguageFragment changeLanguageFragment;
    private TimingOffFragment timingOffFragment;
    private WoodenFishFragment woodenFishFragment;
    private AboutFragment aboutFragment;
    private SettingsFragment mSettingsFragment;
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

    private int seekbarProgress = 0;
    private String currentMusicInfo = "";
    private String currentPlayTime = "00:00";
    private String mediaTime = "";
    private MusicPlayService musicService;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugLog.debug("");
        mContext = this;
        // 让状态栏保持可见
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 设置颜色为透明
        StatusBar statusBar = new StatusBar(MainActivity.this);
        statusBar.setColor(R.color.transparent);

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

            /**
             * 这个Intent可以作为选择文件的返回操作逻辑
             * Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
             * openFileManagerIntent.launch(intent);
             * startActivity(intent);
             * */
            openFileManagerIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null){
                        // 获取用户选择的文件夹的 Uri
                        Uri treeUri = result.getData().getData();

                        // 构建打开文件夹的 Intent
                        Intent openFolderIntent = new Intent(Intent.ACTION_VIEW);
                        openFolderIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        openFolderIntent.setData(treeUri);

                        // 启动文件管理器并打开指定文件夹
//                        startActivity(openFolderIntent);
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

    private void initData(){
        // 判断完权限后再启动DataRefreshService
        Intent dataRefreshService = new Intent(context, DataRefreshService.class);
        startService(dataRefreshService);
        // DataRefreshService还没有完全起来，需要延迟一会再继续初始化
        handler.sendEmptyMessageDelayed(Constant.HANDLER_MESSAGE_DELAY_INIT_MAIN_ACTIVITY,600);
    }

    /**
     * 初始化操作(音乐资源、Ui布局)
     * @author wm
     * @createTime 2023/8/24 18:08
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initDataDelay() {
        // 初始化音乐列表资源
        // 这里不能将初始化操作放到DataRefreshService的onCreate中操作，因为首次启动app时可能获取到的列表为空
        DataRefreshService.initResource();

        // 获取播放相关的参数
        initInfo();
        // 创建悬浮窗视图
        createFloatView();
        // 创建Fragment
        createFragment();

        // MusicPlayService服务的启动不受权限影响，交由Application启动，验证完权限这些之后初始化即可
        musicService = BasicApplication.getMusicService();
        if (musicService != null) {
            // service创建成功的时候立即初始化
            musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);
            musicService.initPlayHelper(handler);
            // 需要等musicService起来之后再给Fragment传参数
            mainViewFragment.setDataFromMainActivity(musicService, handler);
            musicPlayFragment.setDataFromMainActivity(musicService, handler, musicListName, mPosition);
        } else {
            // musicService初始化失败，退出app
            Toast.makeText(context,"启动过程中遇到了异常，即将退出！", Toast.LENGTH_LONG).show();
            closeApp();
        }
        registerReceiver();
    }

    /**
     *  初始化一些必要的参数
     *  @author wm
     *  @createTime 2023/9/27 0:25
     */
    private void initInfo(){
        // 从DataRefreshService中获取音乐列表，上次播放的信息等
        playMode = DataRefreshService.getLastPlayMode();
        musicListName = DataRefreshService.getLastPlayListName();
        mPosition = DataRefreshService.getLastPosition();
        musicInfo = DataRefreshService.getMusicListByName(musicListName);
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        historyList = DataRefreshService.getHistoryList();

        // 这里需要初始化currentMusicInfo这几个属性，避免打开app没有播放音乐进去播放页面时，音乐信息为空白
        // seekbarProgress不需要初始化，默认就是0, currentPlayTime 默认是 "00:00";
        currentMusicInfo = musicInfo.get(mPosition).getTitle();
        mediaTime = ToolsUtils.getInstance().formatTime(musicInfo.get(mPosition).getDuration());
    }

    /**
     *  创建Fragment，并初始化
     *  @author wm
     *  @createTime 2023/9/27 0:23
     */
    private void createFragment(){
        // 创建Fragment实例，并加载显示MainViewFragment
        SplashFragment splashFragment = SplashFragment.getInstance(mContext);
        mainViewFragment = MainViewFragment.getInstance(mContext);
        musicPlayFragment = MusicPlayFragment.getInstance(mContext);
        statisticsFragment = StatisticsFragment.getInstance(mContext);
        changeLanguageFragment = ChangeLanguageFragment.getInstance(mContext);
        timingOffFragment = TimingOffFragment.getInstance(mContext);
        woodenFishFragment = WoodenFishFragment.getInstance(mContext);
        aboutFragment = AboutFragment.getInstance(mContext);
        mSettingsFragment = SettingsFragment.getInstance(mContext);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fl_main_view, splashFragment);
        transaction.commit();

        // 注册截图接口回调
        statisticsFragment.setCaptureCallback(() -> toCapture());
    }

    @Override
    protected void onResume() {
        super.onResume();
        DebugLog.debug("");
        getInfoFromService();
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    /**
     *  从service获取音量信息和当前音乐列表，应对息屏唤醒等的情况
     *  @author wm
     *  @createTime 2024/1/5 17:03
     */
    private void getInfoFromService(){
        if (musicService != null) {
            isPlaying = musicService.isPlaying();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListName = musicService.getMusicListName();
            firstPlay = musicService.getFirstPlay();
        }
    }

    /**
     * 菜单、返回键响应
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mainViewFragment.isVisible()) {
                //调用双击退出函数
                exitBy2Click();
            } else if (musicPlayFragment.isVisible() || statisticsFragment.isVisible()
                    || changeLanguageFragment.isVisible() || timingOffFragment.isVisible()
                    || woodenFishFragment.isVisible() || aboutFragment.isVisible()
                    || mSettingsFragment.isVisible()) {
                returnMainFragment();
            }
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
                getInfoFromService();
                refreshFragmentStatus();
                refreshListStatus();
            } else if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_LIST_STATE) {
                // service发送的信息，收藏/取消收藏歌曲时，刷新列表
                refreshFragmentStatus();
                refreshListStatus();
            } else if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_FROM_PLAY_HELPER) {
                // playHelper发送的消息，用于更新播放进度，目前只用于MusicPlayFragment
                seekbarProgress = msg.getData().getInt("seekbarProgress");
                currentMusicInfo = msg.getData().getString("currentPlayingInfo");
                currentPlayTime = msg.getData().getString("currentTime");
                mediaTime = msg.getData().getString("mediaTime");
                if (musicPlayFragment.isVisible()) {
                    musicPlayFragment.refreshCurrentPlayInfo(seekbarProgress, currentMusicInfo, currentPlayTime, mediaTime);
                }
            } else if (msg.what == Constant.HANDLER_MESSAGE_SHOW_LIST_FRAGMENT) {
                // 显示音乐列表悬浮窗
                showFloatView();
            } else if (msg.what == Constant.HANDLER_MESSAGE_DELAY_INIT_MAIN_ACTIVITY) {
                // 权限申请操作完成后，此时service可能没有完全起来，需要延时一会才能获取service对象进行初始化
                initDataDelay();
            } else if (msg.what == Constant.HANDLER_MESSAGE_DELAY_INIT_FRAGMENT) {
                // 切换Fragment后，延迟一会再设置相关参数
                if (musicPlayFragment.isVisible()) {
                    // 同步当前播放进度，解决在主页先播放然后暂停，进入播放页后进度条仍未0的问题
                    musicPlayFragment.refreshCurrentPlayInfo(seekbarProgress, currentMusicInfo, currentPlayTime, mediaTime);
                }
            } else if (msg.what == Constant.HANDLER_MESSAGE_DELAY_TIMING_OFF) {
                // 定时关闭
                showCloseDialog();
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
        PlayListFragment defaultListFragment = new PlayListFragment(mContext, musicInfo, musicListName, Constant.LIST_SHOW_MODE_CURRENT);
        PlayListFragment favoriteListFragment = new PlayListFragment(mContext, favoriteList, Constant.LIST_MODE_FAVORITE_NAME, Constant.LIST_SHOW_MODE_FAVORITE);
        PlayListFragment historyListFragment = new PlayListFragment(mContext, historyList, Constant.LIST_MODE_HISTORY_NAME, Constant.LIST_SHOW_MODE_HISTORY);
        viewPagerLists = new ArrayList<>();
        viewPagerLists.add(defaultListFragment);
        viewPagerLists.add(favoriteListFragment);
        viewPagerLists.add(historyListFragment);
        ListViewPagerAdapter listViewPagerAdapter = new ListViewPagerAdapter((FragmentActivity) mContext, viewPagerLists);
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
            // 规定第一个FragmentList就是动态列表的，所以直接用get(0)获取第一个页面
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
            mainViewFragment.refreshCustomerList();
        }
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

                // 更新各个Fragment的数据
                refreshFragmentStatus();
                // 刷新播放列表的高亮状态
                refreshListStatus();
            } else if(Constant.REFRESH_SEARCH_RESULT_ACTION.equals(intent.getAction())) {
                // DataRefreshService发送的广播，搜索操作结束后通知Activity更新MainViewFragment的搜索结果
                if (mainViewFragment.isVisible()){
                    mainViewFragment.refreshSearchResult();
                }
            } else if(Constant.CHANGE_FRAGMENT_ACTION.equals(intent.getAction())) {
                // 切换Fragment的广播
                String fragmentName = intent.getExtras().getString("fragment");
                DebugLog.debug("action fragment "  + fragmentName);
                if (Constant.STATISTICS_FRAGMENT_ACTION_FLAG.equals(fragmentName)) {
                    changeFragment(statisticsFragment);
                } else if (Constant.MUSIC_PLAY_FRAGMENT_ACTION_FLAG.equals(fragmentName)) {
                    changeFragment(musicPlayFragment);
                } else if (Constant.CHANGE_LANGUAGE_FRAGMENT_ACTION_FLAG.equals(fragmentName)) {
                    changeFragment(changeLanguageFragment);
                } else if (Constant.TIMING_OFF_FRAGMENT_ACTION_FLAG.equals(fragmentName)) {
                    changeFragment(timingOffFragment);
                } else if (Constant.WOODEN_FISH_FRAGMENT_ACTION_FLAG.equals(fragmentName)) {
                    changeFragment(woodenFishFragment);
                } else if (Constant.ABOUT_FRAGMENT_ACTION_FLAG.equals(fragmentName)) {
                    changeFragment(aboutFragment);
                } else if (Constant.SETTINGS_FRAGMENT_ACTION_FLAG.equals(fragmentName)) {
                    changeFragment(mSettingsFragment);
                } else if (Constant.TOOLS_FRAGMENT_ACTION_FLAG.equals(fragmentName)) {
                    if (mainViewFragment.isVisible()){
                        mainViewFragment.changeToToolsFragment();
                    }
                }
                handler.sendEmptyMessageDelayed(Constant.HANDLER_MESSAGE_DELAY_INIT_FRAGMENT,200);
            } else if(Constant.RETURN_MAIN_VIEW_ACTION.equals(intent.getAction())) {
                // 返回或进入MainViewFragment的广播
                returnMainFragment();
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
        filter.addAction(Constant.REFRESH_SEARCH_RESULT_ACTION);
        filter.addAction(Constant.CHANGE_FRAGMENT_ACTION);
        filter.addAction(Constant.RETURN_MAIN_VIEW_ACTION);
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

    /**
     *  返回MainViewFragment的操作
     *  @author wm
     *  @createTime 2023/9/24 0:06
     */
    private void returnMainFragment(){
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fl_main_view, mainViewFragment);
            transaction.commit();
        } catch (Exception exception){
            DebugLog.debug(exception.getMessage());
        }
    }

    /**
     *  切换其他Fragment的操作
     *  @author wm
     *  @createTime 2023/9/24 0:14
     * @param fragment: 要切换的Fragment
     */
    private void changeFragment(Fragment fragment){
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.addToBackStack(null);
            transaction.replace(R.id.fl_main_view, fragment);
            transaction.commit();
        } catch (Exception exception){
            DebugLog.debug(exception.getMessage());
        }
    }


    private CountDownTimer countDownTimer;
    /**
     *  显示关闭应用的弹框
     *  @author wm
     *  @createTime 2023/9/28 20:06
     */
    private void showCloseDialog(){
        // 创建自定义布局
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_countdown, null);
        TextView countdownTextView = dialogView.findViewById(R.id.tv_countdownTextView);

        // 创建 AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("定时关闭功能");
        builder.setMessage("(取消后重新计时)");
        // 设置自定义布局
        builder.setView(dialogView);
        // 获取弹框的布局视图
        AlertDialog alertDialog = builder.create();

        // 设置弹框上的确定按钮点击事件
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "确定", (dialog, which) -> {
            // 停止倒计时并关闭弹框
            stopCountdown();
            dialog.dismiss();
            closeApp();
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", (dialog, which) -> {
            // 停止倒计时，关闭弹框，重新计时
            stopCountdown();
            dialog.dismiss();
            Intent intent = new Intent(Constant.CHANGE_TIMING_OFF_TIME_ACTION);
            mContext.sendBroadcast(intent);
        });

        // 倒计时总时间为10秒，每隔1秒触发一次
        countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownTextView.setText("即将关闭应用 : " + millisUntilFinished / 1000 + "秒");
                DebugLog.debug("count " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                // 倒计时结束时的操作
                alertDialog.dismiss();
                closeApp();
            }
        };
        // 启动倒计时
        countDownTimer.start();
        // 显示弹框
        alertDialog.show();
    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            // 取消倒计时
            countDownTimer.cancel();
        }
    }

    private void closeApp() {
        BasicApplication.getActivityManager().finishAll();
    }
    
    /**
     *  截图功能：通过画布将当前页面变成bitmap，直接加载到ImageView中就能看到效果
     *  优点：实现简单、无权限要求
     *  缺点：
     *     只能截应用页面，状态栏不会被截进去，对于全屏截图的需求不适用
     *     因为getWindow()是Activity中的方法，所以只能在Activity中调用（无法后台截屏）
     *  @author wm
     *  @createTime 2023/11/24 14:48
     */
    private void toCapture(){
        View v = getWindow().getDecorView();
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        v.draw(canvas);
        // 将截图结果传给分享页面
        statisticsFragment.setCaptureImage(bitmap);
    }


    /*
    暂存
    *   打印控制逻辑
//            boolean isOpenLog = (Boolean) SharedPreferencesUtil.getData(Constant.LOG_SWITCH,true);
//            DebugLog.debug("isOpenLog " + isOpenLog);
//            boolean result = SharedPreferencesUtil.putData(Constant.LOG_SWITCH, !isOpenLog);

//            boolean writeToFile = (Boolean) SharedPreferencesUtil.getData(Constant.LOG_WRITE, true);
//            DebugLog.debug("isOpenLog " + writeToFile);
//            boolean result = SharedPreferencesUtil.putData(Constant.LOG_WRITE, !writeToFile);
    *
    * */

}
