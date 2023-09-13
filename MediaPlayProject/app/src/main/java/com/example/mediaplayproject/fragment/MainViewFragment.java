package com.example.mediaplayproject.fragment;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.MainActivity;
import com.example.mediaplayproject.adapter.viewpager.MainViewPagerAdapter;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.fragment.main.DiscoveryFragment;
import com.example.mediaplayproject.fragment.main.PersonalPageFragment;
import com.example.mediaplayproject.fragment.main.ToolsFragment;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.ToolsUtils;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 */
public class MainViewFragment extends Fragment implements NavigationView.OnNavigationItemSelectedListener {

    private final Context mContext;
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

    private EditText etSearch;
    private ImageView ivSettings, ivSearch, ivPlayMusic, ivMusicList, ivDiscovery, ivPersonal, ivTools, ivShow;
    private TextView tvCurrentMusicInfo;

    private MainActivity.MyFragmentCallBack myFragmentCallBack;

    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private List<MediaFileBean> defaultList = new ArrayList<>();
    private int mPosition = 0;
    private int playMode = 0;
    private String musicListName = Constant.LIST_MODE_DEFAULT_NAME;
    private boolean firstPlay = true, isPlaying = false;
    private MusicPlayService musicService;

    private Handler mActivityHandle;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_main_view, container, false);
        // 绑定布局资源,只能执行一次，不能放到start或者之后的方法里；
        // 否则会重复创建MusicViewPagerAdapter等操作，引发异常
        initData();
        mainView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ToolsUtils.getInstance().hideKeyboard(mainView);
                return false;
            }
        });
        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        initMusicSource();
    }

    @Override
    public void onResume() {
        super.onResume();
        DebugLog.debug("");
        // 进入界面获取一下音量信息和当前音乐列表
        getInfoFromService();
        // 初始化播放主页的状态
        initPlayStateAndInfo();
    }

    /**
     * 在onResume阶段service还是null
     * 要等Activity获取service之后调用该方法传service过来，再去做service相关的初始化工作
     * 该方法调用时机--处于onResume到Fragment运行的中间阶段
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setDataFromMainActivity(MusicPlayService service, Handler handler, MainActivity.MyFragmentCallBack fragmentCallBack) {
        musicService = service;
        this.mActivityHandle = handler;
        myFragmentCallBack = fragmentCallBack;
        // 进入界面获取一下音量信息和当前音乐列表
        getInfoFromService();
        // 初始化播放主页的状态
        initPlayStateAndInfo();
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
        ivSettings = mainView.findViewById(R.id.iv_setting);
        etSearch = mainView.findViewById(R.id.et_search);
        ivSearch = mainView.findViewById(R.id.iv_search);
        ivPlayMusic = mainView.findViewById(R.id.iv_play_music);
        ivMusicList = mainView.findViewById(R.id.iv_current_list);
        ivDiscovery = mainView.findViewById(R.id.iv_discovery);
        ivPersonal = mainView.findViewById(R.id.iv_personal);
        ivTools = mainView.findViewById(R.id.iv_tools);
        ivShow = mainView.findViewById(R.id.iv_show_list);
        ivSettings.setOnClickListener(mListener);
        etSearch.setOnClickListener(mListener);
        ivSearch.setOnClickListener(mListener);
        ivPlayMusic.setOnClickListener(mListener);
        ivMusicList.setOnClickListener(mListener);
        ivDiscovery.setOnClickListener(mListener);
        ivPersonal.setOnClickListener(mListener);
        ivTools.setOnClickListener(mListener);
        ivShow.setOnClickListener(mListener);

        LinearLayout llSimplePlayView = mainView.findViewById(R.id.ll_simple_play_view);
        llSimplePlayView.setOnClickListener(simplePlayViewListener);

        // 初始化主页ViewPager
        discoveryFragment = DiscoveryFragment.getInstance();
        personalPageFragment = PersonalPageFragment.getInstance(mContext);
        toolsFragment = ToolsFragment.getInstance();
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
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                ToolsUtils.getInstance().hideKeyboard(mainView);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

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
            ivPlayMusic.setImageResource(musicService.isPlaying() ? R.mipmap.media_pause : R.mipmap.media_play);
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

        ivPlayMusic.setImageResource(isPlaying ? R.mipmap.media_pause : R.mipmap.media_play);
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
        }
        musicService.play(mediaFileBean, isRestPlayer, mPosition);
    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivSettings) {
            ToolsUtils.getInstance().hideKeyboard(mainView);
            // 打开侧滑栏
            drawerLayout.openDrawer(GravityCompat.START);
        } else if (view == ivSearch) {
            ToolsUtils.getInstance().hideKeyboard(mainView);
        } else if (view == ivPlayMusic) {
            // 需要用firstPlay来判断当前是否是首次播放
            toPlayMusic(musicInfo.get(mPosition), firstPlay);
            firstPlay = false;
        } else if (view == ivDiscovery) {
        } else if (view == ivPersonal) {
        } else if (view == ivTools) {
        } else if (view == ivShow) {
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
        myFragmentCallBack.changeFragment();
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
    public void refreshPlayState(boolean isPlaying, int mPosition, String musicListName, List<MediaFileBean> musicInfo, boolean firstPlay) {
        this.isPlaying = isPlaying;
        this.mPosition = mPosition;
        this.musicListName = musicListName;
        this.musicInfo = musicInfo;
        this.firstPlay = firstPlay;

        // 刷新播放状态信息
        initPlayStateAndInfo();
    }


    public void refreshCustomerList() {
        personalPageFragment.refreshCustomerList();
    }

}