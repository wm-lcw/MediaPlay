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

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.MainActivity;
import com.example.mediaplayproject.adapter.MusicViewPagerAdapter;
import com.example.mediaplayproject.base.BaseFragment;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 */
public class MainViewFragment extends Fragment implements NavigationView.OnNavigationItemSelectedListener, View.OnTouchListener {

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

    private MusicViewPagerAdapter mainViewPagerAdapter;
    private ViewPager2 musicListViewPager;
    private ArrayList<BaseFragment> mainViewPagerLists;
    private DiscoveryFragment discoveryFragment;
    private PersonalPageFragment personalPageFragment;
    private ToolsFragment toolsFragment;

    private LinearLayout llSimplePlayView;
    private ImageView ivPlayMusic, ivMusicList;
    private TextView tvCurrentMusicInfo;

    private MainActivity.MyFragmentCallBack myFragmentCallBack;

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

    public MainViewFragment(Context context) {
        mContext = context;
    }

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
        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        initMusicSource();
        initData();
    }

    /**
     * 执行顺序：
     * onStart()-->setDataFromMainActivity()-->onResume()
     */


    @Override
    public void onResume() {
        super.onResume();
        // 重新进入界面之后都获取一下音量信息和当前音乐列表
        if (musicService != null) {
            //再次进入界面时刷新播放状态按钮，初次进入默认为暂停状态
            ivPlayMusic.setImageResource(musicService.isPlaying() ? R.mipmap.media_pause : R.mipmap.media_play);
            //isInitPlayHelper：是否已经初始化; firstPlay :首次播放
            isInitPlayHelper = musicService.getInitResult();
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListMode = musicService.getMusicListMode();
        }

        switchMusicList();

        //初始化播放主页的状态
        initPlayStateAndInfo();
    }

    private void initMusicSource() {
        // 从BasicApplication中获取音乐列表，上次播放的信息等
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        playMode = DataRefreshService.getLastPlayMode();
        musicListMode = DataRefreshService.getLastPlayListMode();
        mPosition = DataRefreshService.getLastPosition();
    }

    private void initData() {
        // 绑定布局资源
        mainView.findViewById(R.id.bt_music).setOnClickListener(mListener);
        mainView.findViewById(R.id.iv_setting).setOnClickListener(mListener);
        musicListViewPager = mainView.findViewById(R.id.main_view_pager);
        drawerLayout = mainView.findViewById(R.id.drawer_layout);
        tvCurrentMusicInfo = mainView.findViewById(R.id.tv_current_music_info);
        ivPlayMusic = mainView.findViewById(R.id.iv_play_music);
        ivMusicList = mainView.findViewById(R.id.iv_current_list);
        ivPlayMusic.setOnClickListener(mListener);
        ivMusicList.setOnClickListener(mListener);

        llSimplePlayView = mainView.findViewById(R.id.ll_simple_play_view);
        llSimplePlayView.setOnClickListener(simplePlayViewListener);

        // 初始化主页ViewPager
        discoveryFragment = DiscoveryFragment.getInstance();
        personalPageFragment = PersonalPageFragment.getInstance();
        toolsFragment = ToolsFragment.getInstance();
        mainViewPagerLists = new ArrayList<>();
        mainViewPagerLists.add(discoveryFragment);
        mainViewPagerLists.add(personalPageFragment);
        mainViewPagerLists.add(toolsFragment);
        mainViewPagerAdapter = new MusicViewPagerAdapter((FragmentActivity) mContext, mainViewPagerLists);
        musicListViewPager.setAdapter(mainViewPagerAdapter);

        // 侧滑栏的监听设定
        mGestureDetector = new GestureDetector(mContext, myGestureListener);
        drawerLayout.setOnTouchListener(this);
        // 必须设置setLongClickable为true 否则监听不到手势
        drawerLayout.setLongClickable(true);
    }

    private void switchMusicList() {
        if (musicListMode == 0) {
            musicInfo = defaultList;
        } else if (musicListMode == 1) {
            musicInfo = favoriteList;
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/13 14:56
     * @description 初始化播放主页各按钮的状态
     */
    @SuppressLint("SetTextI18n")
    private void initPlayStateAndInfo() {
        //获取音乐列表之后，若是列表为不空，则将当前下标的歌曲信息显示出来
        if (musicInfo.size() > 0) {
            tvCurrentMusicInfo.setText(musicInfo.get(mPosition).getTitle());
        } else {
            //若列表为空，则播放、上下曲都不可点击
            tvCurrentMusicInfo.setText("");
        }

    }

    private final View.OnClickListener mListener = view -> {
        if (view.getId() == R.id.iv_setting) {
            drawerLayout.openDrawer(GravityCompat.START);
        } else if (view.getId() == R.id.iv_play_music) {

        }
    };

    private final View.OnClickListener simplePlayViewListener = view -> {
        if (view instanceof ImageView) {
            // 点击底部播放控制栏，若是点击到按钮(ImageView)就不处理
            return;
        }

        // 点击到其他地方，就跳转进去MusicPlayFragment页面
        myFragmentCallBack.changeFragment();
    };

    public void setDataFromMainActivity(MusicPlayService service, int listMode, int position, MainActivity.MyFragmentCallBack fragmentCallBack) {
        musicService = service;
        musicListMode = listMode;
        mPosition = position;
        myFragmentCallBack = fragmentCallBack;

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
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
            float x = e1.getX() - e2.getX();
            float x2 = e2.getX() - e1.getX();
            if (x > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                drawerLayout.closeDrawer(GravityCompat.START);

            } else if (x2 > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return false;
        }
    };
}