package com.example.mediaplayproject.fragment;


import static com.example.mediaplayproject.base.BasicApplication.getApplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.musiclist.SearchResultListAdapter;
import com.example.mediaplayproject.adapter.viewpager.MainViewPagerAdapter;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.bean.SearchMusicBean;
import com.example.mediaplayproject.fragment.main.DiscoveryFragment;
import com.example.mediaplayproject.fragment.main.PersonalPageFragment;
import com.example.mediaplayproject.fragment.main.ToolsFragment;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.ToolsUtils;
import com.example.mediaplayproject.view.CustomizeEditText;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 */
public class MainViewFragment extends Fragment implements NavigationView.OnNavigationItemSelectedListener {

    private Context mContext;
    private View mainView;

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

    private MainViewPagerAdapter mainViewPagerAdapter;
    private ViewPager2 musicListViewPager;
    private ArrayList<Fragment> mainViewPagerLists;
    private DiscoveryFragment discoveryFragment;
    private PersonalPageFragment personalPageFragment;
    private ToolsFragment toolsFragment;

    private CustomizeEditText customizeEditText;
    private ImageView ivSettings, ivSearch, ivPlayMusic, ivMusicList, ivDiscovery, ivPersonal, ivTools, ivPlayRevolve;
    private TextView tvCurrentMusicInfo;


    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private List<MediaFileBean> defaultList = new ArrayList<>();
    private int mPosition = 0;
    private int playMode = 0;
    private String musicListName = Constant.LIST_MODE_DEFAULT_NAME;
    private boolean firstPlay = true, isPlaying = false;
    private MusicPlayService musicService;

    private Handler mActivityHandle;
    private Animation animation;

    private List<SearchMusicBean> searchResultList = new ArrayList<>();
    private ImageView ivCloseSearch;
    private RecyclerView searchResultRecyclerView;
    private SearchResultListAdapter searchResultListAdapter;
    private RelativeLayout mFloatLayout;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private EditText searchEditText;

    final Handler mainViewHandler = new Handler(Looper.myLooper()) {
        @SuppressLint("ResourceAsColor")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == Constant.HANDLER_MESSAGE_DELAY_OPEN_KEYBOARD) {
                ToolsUtils.getInstance().showKeyBoard(searchEditText);
            }
        }
    };

    public MainViewFragment(Context context) {
        mContext = context;
    }

    @SuppressLint("StaticFieldLeak")
    private static MainViewFragment instance;

    public static MainViewFragment getInstance(Context context) {
        if (instance == null) {
            instance = new MainViewFragment(context);
        }
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_main_view, container, false);
        // 绑定布局资源,只能执行一次，不能放到start或者之后的方法里；
        // 否则会重复创建MusicViewPagerAdapter等操作，引发异常
        initData();
        createFloatView();
        mainView.setOnTouchListener((View v,  MotionEvent event) -> {
            ToolsUtils.getInstance().hideKeyboard(mainView);
            return false;
        });
        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        initMusicSource();
    }

    /**
     *  初始化逻辑修改后，在onResume阶段musicService已经启动了，
     *  @author wm
     *  @createTime 2023/9/18 0:02
     */
    @Override
    public void onResume() {
        super.onResume();
        DebugLog.debug("");
        // 进入界面获取一下音量信息和当前音乐列表
        getInfoFromService();
        // 初始化播放主页的状态
        initPlayStateAndInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWindowManager != null && mFloatLayout.isAttachedToWindow()) {
            // 在Fragment失去焦点时，悬浮窗关闭, 需要判断mFloatLayout正在显示才执行移除，否则会报错
            mWindowManager.removeView(mFloatLayout);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setDataFromMainActivity(MusicPlayService service, Handler handler) {
        musicService = service;
        this.mActivityHandle = handler;
    }

    /**
     * 从BasicApplication中获取音乐列表，上次播放的信息等
     *
     * @author wm
     * @createTime 2023/8/24 19:27
     */
    private void initMusicSource() {
        defaultList = DataRefreshService.getDefaultList();
        playMode = DataRefreshService.getLastPlayMode();
        musicListName = DataRefreshService.getLastPlayListName();
        mPosition = DataRefreshService.getLastPosition();
    }

    /**
     * 绑定布局资源,初始化ViewPager等操作
     *
     * @author wm
     * @createTime 2023/8/24 19:22
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        musicListViewPager = mainView.findViewById(R.id.main_view_pager);
        drawerLayout = mainView.findViewById(R.id.drawer_layout);
        tvCurrentMusicInfo = mainView.findViewById(R.id.tv_current_music_info);
        customizeEditText = mainView.findViewById(R.id.custom_edit_text);
        searchEditText = customizeEditText.getEditText();
        ivSearch = customizeEditText.getSearchImageView();
        ivSettings = mainView.findViewById(R.id.iv_setting);
        ivPlayMusic = mainView.findViewById(R.id.iv_play_music);
        ivMusicList = mainView.findViewById(R.id.iv_current_list);
        ivDiscovery = mainView.findViewById(R.id.iv_discovery);
        ivPersonal = mainView.findViewById(R.id.iv_personal);
        ivTools = mainView.findViewById(R.id.iv_tools);
        ivPlayRevolve = mainView.findViewById(R.id.iv_play_revolve);
        ivSettings.setOnClickListener(mListener);
        ivSearch.setOnClickListener(mListener);
        ivPlayMusic.setOnClickListener(mListener);
        ivMusicList.setOnClickListener(mListener);
        ivDiscovery.setOnClickListener(mListener);
        ivPersonal.setOnClickListener(mListener);
        ivTools.setOnClickListener(mListener);

        searchEditText.setOnClickListener(mListener);

        LinearLayout llSimplePlayView = mainView.findViewById(R.id.ll_simple_play_view);
        llSimplePlayView.setOnClickListener(simplePlayViewListener);

        // 初始化主页ViewPager
        discoveryFragment = DiscoveryFragment.getInstance(mContext);
        personalPageFragment = PersonalPageFragment.getInstance(mContext);
        toolsFragment = ToolsFragment.getInstance(mContext);
        mainViewPagerLists = new ArrayList<>();
        mainViewPagerLists.add(discoveryFragment);
        mainViewPagerLists.add(personalPageFragment);
        mainViewPagerLists.add(toolsFragment);
        mainViewPagerAdapter = new MainViewPagerAdapter((FragmentActivity) mContext, mainViewPagerLists);
        musicListViewPager.setAdapter(mainViewPagerAdapter);
        musicListViewPager.setCurrentItem(1);

        musicListViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                ToolsUtils.getInstance().hideKeyboard(mainView);

                ivDiscovery.setImageResource(position == 0 ? R.mipmap.ic_discovery_pre : R.mipmap.ic_discovery_nor);
                ivPersonal.setImageResource(position == 1 ? R.mipmap.ic_customer_pre : R.mipmap.ic_customer_nor);
                ivTools.setImageResource(position == 2 ? R.mipmap.ic_tools_pre : R.mipmap.ic_tools_nor);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                ToolsUtils.getInstance().hideKeyboard(mainView);
                ivDiscovery.setImageResource(position == 0 ? R.mipmap.ic_discovery_pre : R.mipmap.ic_discovery_nor);
                ivPersonal.setImageResource(position == 1 ? R.mipmap.ic_customer_pre : R.mipmap.ic_customer_nor);
                ivTools.setImageResource(position == 2 ? R.mipmap.ic_tools_pre : R.mipmap.ic_tools_nor);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        // 设置图标旋转的动画
        animation = AnimationUtils.loadAnimation(mContext, R.anim.ic_playing_animation);
        //设置动画匀速运动
        // setInterpolator表示设置旋转速率。
        // LinearInterpolator为匀速效果，AccelerateInterpolator为加速效果，DecelerateInterpolator为减速效果
        LinearInterpolator lin = new LinearInterpolator();
        animation.setInterpolator(lin);




        // 侧滑栏的滑动唤出效果可以不需要了，点击按钮能唤出来，点击空白能收回去即可，代码先保留

//        // 侧滑栏的监听设定
//        mGestureDetector = new GestureDetector(mContext, myGestureListener);
//        drawerLayout.setOnTouchListener((v, event) -> {
//            // 处理DrawerLayout的onTouch事件
//            // 这里直接调用GestureDetector.SimpleOnGestureListener的onTouchEvent方法来处理
//            return mGestureDetector.onTouchEvent(event);
//        });
//        // 必须设置setLongClickable为true 否则监听不到手势
//        drawerLayout.setLongClickable(true);
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
            float x = e1.getX() - e2.getX();
            float x2 = e2.getX() - e1.getX();
            DebugLog.debug("x " + x + "; x2 " + x2);
            if (x > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (x2 > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return false;
        }
    };

    /**
     * 从service中获取音量信息和当前音乐列表
     * @author wm
     * @createTime 2023/8/24 19:28
     */
    private void getInfoFromService() {
        if (musicService != null) {
            ivPlayMusic.setImageResource(musicService.isPlaying() ? R.mipmap.ic_main_view_pause_grey : R.mipmap.ic_main_view_play_grey);
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            isPlaying = musicService.isPlaying();
            musicListName = musicService.getMusicListName();
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/13 14:56
     * @description 初始化播放主页各按钮的状态
     */
    @SuppressLint("SetTextI18n")
    private void initPlayStateAndInfo() {
        musicInfo = DataRefreshService.getMusicListByName(musicListName);
        //获取音乐列表之后，若是列表为不空，则将当前下标的歌曲信息显示出来
        if (musicInfo.size() > 0) {
            tvCurrentMusicInfo.setText(musicInfo.get(mPosition).getTitle());
            ivPlayMusic.setEnabled(true);
        } else {
            // 若列表为空，则播放、上下曲都不可点击
            tvCurrentMusicInfo.setText("");
            ivPlayMusic.setEnabled(false);
        }

        ivPlayMusic.setImageResource(isPlaying ? R.mipmap.ic_main_view_pause_grey : R.mipmap.ic_main_view_play_grey);
        // 设置旋转图标的状态
        if (isPlaying){
            ivPlayRevolve.startAnimation(animation);
        } else {
            ivPlayRevolve.clearAnimation();
        }

        // 搜索框默认显示第一首音乐名称
        if (defaultList.size() > 0) {
            DebugLog.debug("search");
            customizeEditText.setEditTextHint(defaultList.get(0).getTitle());
        }
    }

    /**
     *  播放音乐
     *  @author wm
     *  @createTime 2023/9/3 17:48
     * @param mediaFileBean: 即将播放的音乐对象
     * @param isRestPlayer: 是否需要重头开始播放
     */
    private void toPlayMusic(MediaFileBean mediaFileBean, Boolean isRestPlayer) {
        if (musicService != null) {
            // 播放前先初始化service
            musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);
            musicService.play(mediaFileBean, isRestPlayer, mPosition);
        }
    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivSettings) {
            ToolsUtils.getInstance().hideKeyboard(mainView);
            // 打开侧滑栏
            drawerLayout.openDrawer(GravityCompat.START);
        } else if (view == searchEditText) {
            // 点击搜索框，先隐藏搜索结果，再唤出输入法
            if (mWindowManager != null && mFloatLayout.isAttachedToWindow()) {
                mWindowManager.removeView(mFloatLayout);
            }
            // 延迟一点再唤出键盘，避免结果的视图还没消失就唤出键盘，造成键盘唤出失败
            mainViewHandler.sendEmptyMessageDelayed(Constant.HANDLER_MESSAGE_DELAY_OPEN_KEYBOARD,200);
        } else if (view == ivSearch) {
            ToolsUtils.getInstance().hideKeyboard(mainView);
            if (customizeEditText != null){
                String inputText = customizeEditText.getText().trim();
                if (!"".equals(inputText)) {
                    mWindowManager.addView(mFloatLayout, wmParams);
                    DataRefreshService.searchMusic(Constant.SEARCH_ALL_MUSIC_FLAG, "ALL", inputText);
                }
            }
        } else if (view == ivCloseSearch) {
            searchResultList = null;
            if (mWindowManager != null && mFloatLayout.isAttachedToWindow()) {
                // 在Fragment失去焦点时，悬浮窗关闭, 需要判断mFloatLayout正在显示才执行移除，否则会报错
                mWindowManager.removeView(mFloatLayout);
            }
        } else if (view == ivPlayMusic) {
            // 需要用firstPlay来判断当前是否是首次播放
            toPlayMusic(musicInfo.get(mPosition), firstPlay);
            firstPlay = false;
        } else if (view == ivDiscovery) {
            musicListViewPager.setCurrentItem(0);
        } else if (view == ivPersonal) {
            musicListViewPager.setCurrentItem(1);
        } else if (view == ivTools) {
            musicListViewPager.setCurrentItem(2);
        } else if (view == ivMusicList) {
            // 主页展示播放列表
            Message msg = new Message();
            msg.what = Constant.HANDLER_MESSAGE_SHOW_LIST_FRAGMENT;
            mActivityHandle.sendMessage(msg);
        }
    };

    private final View.OnClickListener simplePlayViewListener = view -> {
        if (view instanceof ImageView) {
            // 点击底部播放控制栏，若是点击到自定义的按钮(ImageView)就不处理，在其他地方单独处理
            return;
        }

        // 点击到其他区域，就跳转进去MusicPlayFragment页面
        Intent intent = new Intent(Constant.CHANGE_FRAGMENT_ACTION);
        Bundle bundle = new Bundle();
        bundle.putString("fragment", Constant.MUSIC_PLAY_FRAGMENT_ACTION_FLAG);
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);
    };


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    /**
     * 更改播放状态和信息，Activity接收到service发出的更新通知后调用
     * @author wm
     * @createTime 2023/8/24 19:23
     */
    @SuppressLint("NotifyDataSetChanged")
    public void refreshPlayState(boolean isPlaying, int mPosition, String musicListName, List<MediaFileBean> musicInfo, boolean firstPlay) {
        this.isPlaying = isPlaying;
        this.mPosition = mPosition;
        this.musicListName = musicListName;
        this.musicInfo = musicInfo;
        this.firstPlay = firstPlay;

        // 刷新播放状态信息
        initPlayStateAndInfo();

        // 刷新搜索结果的高亮显示
        if (searchResultListAdapter != null){
            searchResultListAdapter.notifyDataSetChanged();
        }
    }


    public void refreshCustomerList() {
        personalPageFragment.refreshCustomerList();
    }

    /**
     *  创建搜索结果悬浮窗
     *  @author wm
     *  @createTime 2023/9/20 21:25
     */
    private void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        // 获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        // 设置window type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        // 设置背景为透明，否则滑动ListView会出现残影
        wmParams.format = PixelFormat.TRANSPARENT;
        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口,不设置这个flag的话，home页的划屏会有问题
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        // 调整悬浮窗显示的停靠位置为顶部和水平方向上居中显示
        wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 160;

        // 设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        initFloatView();
    }

    /**
     *  初始化悬浮窗中的视图资源等
     *  @author wm
     *  @createTime 2023/9/3 16:50
     */
    @SuppressLint("InflateParams")
    private void initFloatView() {
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        // 获取浮动窗口视图所在布局
        mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.layout_search_result_view, null);
        ivCloseSearch = mFloatLayout.findViewById(R.id.iv_close_search);
        ivCloseSearch.setOnClickListener(mListener);
        searchResultRecyclerView = mFloatLayout.findViewById(R.id.lv_search_result_recycler_view);
        searchResultListAdapter = new SearchResultListAdapter(mContext, searchResultList);
        searchResultRecyclerView.setAdapter(searchResultListAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        searchResultRecyclerView.setLayoutManager(linearLayoutManager);
        // 添加Android自带的分割线
        searchResultRecyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
    }

    /**
     *  刷新搜索结果，DataRefreshService检索操作结束后发送广播给MainActivity，MainActivity再调用本方法
     *  @author wm
     *  @createTime 2023/9/21 14:39
     */
    public void refreshSearchResult(){
        searchResultList = DataRefreshService.getSearchResultList();
        if (mFloatLayout.isAttachedToWindow()){
            searchResultListAdapter.setMusicList(searchResultList);
        }
    }

    public void changeToToolsFragment(){
        if (musicListViewPager != null) {
            musicListViewPager.setCurrentItem(2);
        }
    }

}