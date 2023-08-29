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
import android.os.Looper;
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
import com.example.mediaplayproject.bean.MusicListBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.DebugLog;
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
    private ImageView ivSettings, ivPlayMusic, ivMusicList, ivDiscovery, ivPersonal, ivTools, ivShow;
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

    private boolean firstPlay = true, isInitPlayHelper = false, isPlaying = false;

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
        // 绑定布局资源,只能执行一次，不能放到start或者之后的方法里；
        // 否则会重复创建MusicViewPagerAdapter等操作，引发异常
        initData();
        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        DebugLog.debug("");
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
    public void setDataFromMainActivity(MusicPlayService service, MainActivity.MyFragmentCallBack fragmentCallBack) {
        DebugLog.debug("");
        musicService = service;
        myFragmentCallBack = fragmentCallBack;
        // 进入界面获取一下音量信息和当前音乐列表
        getInfoFromService();
        // 初始化播放主页的状态
        initPlayStateAndInfo();
    }

    /**
     *  从BasicApplication中获取音乐列表，上次播放的信息等
     *  @author wm
     *  @createTime 2023/8/24 19:27
     */
    private void initMusicSource() {
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        playMode = DataRefreshService.getLastPlayMode();
        musicListMode = DataRefreshService.getLastPlayListMode();
        mPosition = DataRefreshService.getLastPosition();
    }

    /**
     *  绑定布局资源,初始化ViewPager等操作
     *  @author wm
     *  @createTime 2023/8/24 19:22
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initData() {
        musicListViewPager = mainView.findViewById(R.id.main_view_pager);
        drawerLayout = mainView.findViewById(R.id.drawer_layout);
        tvCurrentMusicInfo = mainView.findViewById(R.id.tv_current_music_info);
        ivSettings = mainView.findViewById(R.id.iv_setting);
        ivPlayMusic = mainView.findViewById(R.id.iv_play_music);
        ivMusicList = mainView.findViewById(R.id.iv_current_list);
        ivDiscovery = mainView.findViewById(R.id.iv_discovery);
        ivPersonal = mainView.findViewById(R.id.iv_personal);
        ivTools = mainView.findViewById(R.id.iv_tools);
        ivShow = mainView.findViewById(R.id.iv_show_list);
        ivSettings.setOnClickListener(mListener);
        ivPlayMusic.setOnClickListener(mListener);
        ivMusicList.setOnClickListener(mListener);
        ivDiscovery.setOnClickListener(mListener);
        ivPersonal.setOnClickListener(mListener);
        ivTools.setOnClickListener(mListener);
        ivShow.setOnClickListener(mListener);
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

    /**
     *  从service中获取音量信息和当前音乐列表
     *  @author wm
     *  @createTime 2023/8/24 19:28
     */
    private void getInfoFromService(){
        if (musicService != null) {
            ivPlayMusic.setImageResource(musicService.isPlaying() ? R.mipmap.media_pause : R.mipmap.media_play);
            isInitPlayHelper = musicService.getInitResult();
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            isPlaying = musicService.isPlaying();
            musicListMode = musicService.getMusicListMode();
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/13 14:56
     * @description 初始化播放主页各按钮的状态
     */
    @SuppressLint("SetTextI18n")
    private void initPlayStateAndInfo() {
        if (musicListMode == 0) {
            musicInfo = defaultList;
        } else if (musicListMode == 1) {
            musicInfo = favoriteList;
        }

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

    private void toPlayMusic(MediaFileBean mediaFileBean, Boolean isRestPlayer) {
        initServicePlayHelper();
        DebugLog.debug("service " + musicService);
        musicService.play(mediaFileBean, isRestPlayer, mPosition);
    }

    private void initServicePlayHelper() {
        if (musicService != null) {
            //刷新Service里面的内容时，不用每次都初始化，最主要的是更新position和musicInfo
            //初始化的时候需要先调用initPlayData方法更新各项数据，避免数组越界
            musicService.initPlayData(musicInfo, mPosition, musicListMode, playMode);
        }
    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivSettings) {
            drawerLayout.openDrawer(GravityCompat.START);
        } else if (view == ivPlayMusic) {
            // 判断当前是否是首次播放，若是首次播放，则需要设置重头开始播放（Media的首次播放需要reset等流程）
            toPlayMusic(musicInfo.get(mPosition), firstPlay);
            firstPlay = false;
        } else if (view == ivDiscovery){
            // create musicList
            DataRefreshService.createNewMusicList("myList");

        } else if (view == ivPersonal){
            // insert music to musicList
            List<Long> myListMusic = new ArrayList<>();
            for (int i = 0; i<5;i++){
                myListMusic.add(defaultList.get(i).getId());
            }
            DataRefreshService.insertCustomerMusic("myList",myListMusic);

        } else if (view == ivTools){
            // delete musicList
            List<Long> myListMusic = new ArrayList<>();
            for (int i = 0; i<2;i++){
                myListMusic.add(defaultList.get(i).getId());
            }
            DataRefreshService.deleteCustomerMusic("myList",myListMusic);
        } else if (view == ivShow){
            List<MusicListBean> customerList = DataRefreshService.getCustomerList();
            DebugLog.debug(""+customerList);
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

    /**
     *  更改播放状态和信息，Activity接收到service发出的更新通知后调用
     *  @author wm
     *  @createTime 2023/8/24 19:23
     */
    public void setPlayState(boolean isPlaying) {
        ivPlayMusic.setImageResource(isPlaying ? R.mipmap.media_pause : R.mipmap.media_play);
    }


    /*
    * 列表主页面->点击展开列表-传列表给ShowListFragment
    * -->点击列表中的歌曲-->回调Activity-->跳转到PlayFragment直接播放
    * */
}