package com.example.mediaplayproject.fragment;

import static com.example.mediaplayproject.base.BasicApplication.getApplication;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.MusicViewPagerAdapter;
import com.example.mediaplayproject.base.BaseFragment;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicPlayerHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * @author wm
 */
public class MusicPlayFragment extends Fragment {

    private Context mContext;
    private View playView;
    private ImageView ivMediaLoop, ivMediaPre, ivMediaPlay, ivMediaNext, ivMediaList;
    private SeekBar sbVolume, sbProgress;
    private TextView tvCurrentMusicInfo, tvCurrentPlayTime, tvMediaTime;
    private boolean isShowList = false, mRegistered = false, firstPlay = true, isInitPlayHelper = false, isPlaying = false;
    private LinearLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;
    private AudioManager mAudioManager;
    private MusicBroadcastReceiver mMusicBroadcastReceiver;
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private final String VOLUME_CHANGE_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private final String VOLUME_MUTE = "android.media.STREAM_MUTE_CHANGED_ACTION";
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

    private ViewPager2 musicListViewPager;
    private ArrayList<BaseFragment> viewPagerLists;
    private MusicViewPagerAdapter musicViewPagerAdapter;
//    private DefaultListFragment defaultListFragment;
//    private FavoriteListFragment favoriteListFragment;
    private PlayListFragment defaultListFragment,favoriteListFragment;


    /**
     * MusicPlayService对象，控制音乐播放service类
     */
    private MusicPlayService musicService;

    public MusicPlayFragment(Context context) {
        mContext = context;
    }

    private static MusicPlayFragment instance;

    public static MusicPlayFragment getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayFragment(context);
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
        playView = inflater.inflate(R.layout.fragment_music_play, container, false);
        return playView;
    }

    @Override
    public void onStart() {
        super.onStart();
        initMusicSource();
        registerReceiver();
        createFloatView();
        DebugLog.debug("");
    }

    public void setDataFromMainActivity(MusicPlayService service, int listMode, int position) {
        DebugLog.debug("");
        musicService = service;
        musicListMode = listMode;
        mPosition = position;
    }

    @Override
    public void onResume() {
        super.onResume();
        DebugLog.debug("");
        // 绑定控件
        bindView();
        // 从service获取相关信息
        initData();
        // 首次进入app时先刷新一次音乐列表的来源
        switchMusicList();
        //初始化播放主页的状态
        initPlayStateAndInfo();
        // 初始化音量条
        initVolume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    private void initMusicSource() {
        // 从BasicApplication中获取音乐列表，上次播放的信息等
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        playMode = DataRefreshService.getLastPlayMode();
        musicListMode = DataRefreshService.getLastPlayListMode();
        mPosition = DataRefreshService.getLastPosition();
    }

    private void bindView() {
        ivMediaLoop = playView.findViewById(R.id.bt_loop);
        ivMediaPre = playView.findViewById(R.id.bt_pre);
        ivMediaPlay = playView.findViewById(R.id.bt_play);
        ivMediaNext = playView.findViewById(R.id.bt_next);
        ivMediaList = playView.findViewById(R.id.bt_list);
        sbVolume = playView.findViewById(R.id.sb_volume);
        sbProgress = playView.findViewById(R.id.sb_progress);
        tvCurrentMusicInfo = playView.findViewById(R.id.tv_music_info);
        tvCurrentPlayTime = playView.findViewById(R.id.tv_music_current_time);
        tvMediaTime = playView.findViewById(R.id.tv_music_time);

        ivMediaLoop.setOnClickListener(mListener);
        ivMediaPre.setOnClickListener(mListener);
        ivMediaPlay.setOnClickListener(mListener);
        ivMediaNext.setOnClickListener(mListener);
        ivMediaList.setOnClickListener(mListener);
        sbVolume.setOnSeekBarChangeListener(mSeekBarListener);
        sbProgress.setOnSeekBarChangeListener(mSeekBarListener);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

    }



    private void initData() {
        DebugLog.debug("service " + musicService);
        // 重新进入界面之后都获取一下音量信息和当前音乐列表
        if (musicService != null) {
            //再次进入界面时刷新播放状态按钮，初次进入默认为暂停状态
            isPlaying = musicService.isPlaying();
            //isInitPlayHelper：是否已经初始化; firstPlay :首次播放
            isInitPlayHelper = musicService.getInitResult();
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListMode = musicService.getMusicListMode();
            isInitPlayHelper = true;
        }
    }

    private void switchMusicList() {
        if (musicListMode == 0) {
            musicInfo = defaultList;
        } else if (musicListMode == 1) {
            musicInfo = favoriteList;
        }
        //保存上次播放的列表来源
        DataRefreshService.setLastPlayListMode(musicListMode);
        initServicePlayHelper();
    }

    @SuppressLint("SetTextI18n")
    private void initPlayStateAndInfo() {
        //获取音乐列表之后，若是列表为不空，则将当前下标的歌曲信息显示出来
        if (musicInfo.size() > 0) {
            tvCurrentMusicInfo.setText(musicInfo.get(mPosition).getTitle());
            tvCurrentPlayTime.setText("00:00");
            tvMediaTime.setText(MusicPlayerHelper.formatTime(musicInfo.get(mPosition).getDuration()));
            ivMediaPre.setEnabled(true);
            ivMediaPlay.setEnabled(true);
            ivMediaNext.setEnabled(true);
        } else {
            //若列表为空，则播放、上下曲都不可点击
            tvCurrentMusicInfo.setText("");
            tvCurrentPlayTime.setText("");
            tvMediaTime.setText("");
            ivMediaPre.setEnabled(false);
            ivMediaPlay.setEnabled(false);
            ivMediaNext.setEnabled(false);
        }
        ivMediaPlay.setImageResource(isPlaying ? R.mipmap.media_pause : R.mipmap.media_play);

        //初始化播放模式的图标
        if (playMode == 0) {
            ivMediaLoop.setImageResource(R.mipmap.media_loop);
        } else if (playMode == 1) {
            ivMediaLoop.setImageResource(R.mipmap.media_shuffle);
        } else if (playMode == 2) {
            ivMediaLoop.setImageResource(R.mipmap.media_single);
        }
    }

    private void initVolume() {
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //当前设备的
        if (currentVolume >= 0 && currentVolume <= 150) {
            int afterVolume = (int) (currentVolume / 1.5);
            sbVolume.setProgress(afterVolume);
        }
    }

    private void initServicePlayHelper() {
        if (musicService != null) {
            //刷新Service里面的内容时，不用每次都初始化，最主要的是更新position和musicInfo
            //初始化的时候需要先调用initPlayData方法更新各项数据，避免数组越界
            musicService.initPlayData(musicInfo, mPosition, musicListMode, playMode);
        }
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @SuppressLint({"ResourceType", "UseCompatLoadingForColorStateLists"})
        @Override
        public void onClick(View view) {
            if (view == ivMediaLoop) {
                changePlayMode();
            } else if (view == ivMediaPre) {
                initServicePlayHelper();
                musicService.playPre();
            } else if (view == ivMediaPlay) {
//                DebugLog.debug("current position " + mPosition);
                // 判断当前是否是首次播放，若是首次播放，则需要设置重头开始播放（Media的首次播放需要reset等流程）
                toPlayMusic(musicInfo.get(mPosition), firstPlay);
                firstPlay = false;
            } else if (view == ivMediaNext) {
                initServicePlayHelper();
                musicService.playNext();
            } else if (view == ivMediaList) {
                isShowList = true;
                showFloatView();
                refreshListStatus();
            }
        }
    };

    private final SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == sbVolume) {
                int afterVolume = (int) (progress * 1.5);
                if (fromUser) {
                    //手动拖动seekbar时才设置音量（排除外部改变音量的影响）
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, afterVolume, AudioManager.FLAG_PLAY_SOUND);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (seekBar == sbProgress) {
                musicService.removeMessage();
            }

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (seekBar == sbProgress) {
                int progress = seekBar.getProgress();
                int maxProgress = seekBar.getMax();
                musicService.changeSeekbarProgress(progress, maxProgress);
            }

        }
    };

    /**
     * @author wm
     * @createTime 2023/2/3 18:19
     * @description 创建悬浮窗
     */
    private void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        //设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        //设置背景为透明，否则滑动ListView会出现残影
        wmParams.format = PixelFormat.TRANSPARENT;
        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口,不设置这个flag的话，home页的划屏会有问题
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        //调整悬浮窗显示的停靠位置为右侧底部
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 以屏幕右下角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        //设置悬浮窗口长宽数据
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
        musicListViewPager = mFloatLayout.findViewById(R.id.list_view_pager);
        defaultListFragment = new PlayListFragment(mContext, defaultList, Constant.LIST_MODE_DEFAULT,handler);
        favoriteListFragment = new PlayListFragment(mContext, favoriteList, Constant.LIST_MODE_FAVORITE, handler);
        viewPagerLists = new ArrayList<>();
        viewPagerLists.add(defaultListFragment);
        viewPagerLists.add(favoriteListFragment);
        musicViewPagerAdapter = new MusicViewPagerAdapter((FragmentActivity) mContext, viewPagerLists);
        musicListViewPager.setAdapter(musicViewPagerAdapter);
    }

    /**
     * @createTime 2023/8/14 10:19
     * @description 显示悬浮窗
     */
    private void showFloatView() {
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
    }

    /**
     * 点击窗口外部区域关闭列表窗口
     */
    private void setWindowOutTouch() {
        /* 点击窗口外部区域可消除
         将悬浮窗设置为全屏大小，外层有个透明背景，中间一部分视为内容区域,
         所以点击内容区域外部视为点击悬浮窗外部
         其中popupWindowView为全屏，listWindow为列表区域，触摸点没有落在列表区域，则隐藏列表*/
        final View popupWindowView = mFloatLayout.findViewById(R.id.ll_popup_window);
        final View listWindow = mFloatLayout.findViewById(R.id.ll_listWindow);
        popupWindowView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                Rect rect = new Rect();
                listWindow.getGlobalVisibleRect(rect);
                if (!rect.contains(x, y) && isShowList) {
                    mWindowManager.removeView(mFloatLayout);
                }
                return false;
            }
        });
    }


    /**
     * @createTime 2023/2/8 16:55
     * @description 注册音量广播接收器
     */
    public void registerReceiver() {
        mMusicBroadcastReceiver = new MusicBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VOLUME_CHANGE_ACTION);
        filter.addAction(VOLUME_MUTE);
        filter.addAction(Constant.DELETE_MUSIC_ACTION);
        mContext.registerReceiver(mMusicBroadcastReceiver, filter);
        mRegistered = true;
    }


    /**
     * @author wm
     * @createTime 2023/2/3 18:21
     * @description 创建广播接收器，接收音量(媒体音量)改变的广播
     */
    private class MusicBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //媒体音量改变才通知
            if (VOLUME_CHANGE_ACTION.equals(intent.getAction())
                    && (intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC)) {
                int changedVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                Message msg = new Message();
                msg.what = Constant.HANDLER_MESSAGE_REFRESH_VOLUME;
                Bundle bundle = new Bundle();
                bundle.putInt("volume", changedVolume);
                msg.setData(bundle);
                handler.sendMessage(msg);
            } else if (Constant.DELETE_MUSIC_ACTION.equals(intent.getAction())) {
                //删除收藏歌曲（删除的下标小于当前播放的下标）的时候，需要刷新收藏列表的高亮下标
                int deletePosition = intent.getExtras().getInt("musicPosition");
                int listSource = intent.getExtras().getInt("musicListSource");
                dealDeleteMusic(listSource, deletePosition);
            }
        }
    }

    /**
     * @param listSource     音乐列表类型
     * @param deletePosition 删除音乐所在列表的下标
     * @createTime 2023/8/16 22:54
     * @description 删除音乐的处理
     */
    private void dealDeleteMusic(int listSource, int deletePosition) {
        // 删除的音乐列表是当前正在播放的列表
        if (musicListMode == listSource) {
            if (musicInfo.size() <= 0) {
                // 如果列表为空，证明删除的是最后一首歌，列表为空，需要停止播放
                stopPlayByDelete();
            } else {
                // 刷新列表Ui高亮状态
                int result = viewPagerLists.get(listSource).checkRefreshPosition(deletePosition);
                if (result == Constant.RESULT_BEFORE_CURRENT_POSITION) {
                    // 删除的是小于当前播放下标的歌曲，只刷新service中的position即可，UI操作在Fragment已完成
                    mPosition--;
                    musicService.setPosition(mPosition);
                } else if (result == Constant.RESULT_IS_CURRENT_POSITION) {
                    // 删除的是当前的歌曲，需要重新设置高亮
                    // 删除歌曲后，列表整体上移，position指向的是下首歌了，所以需要减一再播放下一曲（直接播放可能会出现越界问题）
                    mPosition--;
                    musicService.setPosition(mPosition);
                    musicService.playNext();
                    mPosition = musicService.getPosition();
                    viewPagerLists.get(listSource).setSelectPosition(mPosition);
                    viewPagerLists.get(listSource).setSelection(mPosition);
                }
            }
        } else {
            switch (listSource) {
                case Constant.LIST_MODE_FAVORITE:
                    if (favoriteList.size() <= 0 && viewPagerLists.size() > 0) {
                        DebugLog.debug("to DefaultList ");
                        toDefaultList();
                    }
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * @createTime 2023/8/14 16:07
     * @description 删除歌曲后-列表为空，需要切换播放列表并刷新通知栏等状态
     */
    private void stopPlayByDelete() {
        DebugLog.debug("stopPlayByDeleteMusic");
        musicService.toStop();
        musicListMode = 0;
        mPosition = 0;
        firstPlay = true;
        isInitPlayHelper = false;
        switchMusicList();
        //UI上刷新播放信息和播放状态
        initPlayStateAndInfo();
        //刷新播放按钮
        ivMediaPlay.setImageResource(R.mipmap.media_play);
        //刷新通知栏的播放按钮状态
        musicService.updateNotificationShow(0, false);
        //保存最后播放歌曲的id--直接保存
        DataRefreshService.setLastMusicId(defaultList.get(mPosition).getId());
        toDefaultList();
    }

    private void toDefaultList() {
        musicListViewPager.setCurrentItem(0);
        musicViewPagerAdapter.notifyItemChanged(0);
    }

    /**
     * @author wm
     * @createTime 2023/2/8 16:55
     * @description 注销音量广播监听器，需要与 registerReceiver 成对使用
     */
    public void unregisterReceiver() {
        if (mRegistered) {
            try {
                mContext.unregisterReceiver(mMusicBroadcastReceiver);
                mMusicBroadcastReceiver = null;
                mRegistered = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("ResourceAsColor")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_VOLUME) {
                //媒体音量发生变化--更新音量条
                int volume = msg.getData().getInt("volume");
                if (volume >= 0 && volume <= 150) {
                    int afterVolume = (int) (volume / 1.5);
                    sbVolume.setProgress(afterVolume);
                }
            } else if (msg.what == Constant.HANDLER_MESSAGE_FROM_LIST_FRAGMENT) {
                DebugLog.debug("HANDLER_MESSAGE_FROM_LIST_FRAGMENT");
                mPosition = msg.getData().getInt("position");
                musicListMode = msg.getData().getInt("musicListMode");
                DebugLog.debug("Fragment position " + mPosition + "; ListMode " + musicListMode);
                switchMusicList();
                if (musicListMode == 0) {
                    toPlayMusic(defaultList.get(mPosition), true);
                } else if (musicListMode == 1) {
                    toPlayMusic(favoriteList.get(mPosition), true);
                }
                firstPlay = false;
                refreshListStatus();
            }
        }
    };

    public void setPlayState(boolean state) {
        // 更新播放状态
        isPlaying = state;
        ivMediaPlay.setImageResource(isPlaying ? R.mipmap.media_pause : R.mipmap.media_play);
    }

    public void setPositionByServiceListChange(int position) {
        mPosition = position;
        // 更新列表高亮状态
        refreshListStatus();
    }

    public void setCurrentPlayInfo(int seekbarProgress, String currentMusicInfo, String currentPlayTime, String mediaTime) {
        // 刷新播放进度及时间
        sbProgress.setProgress(seekbarProgress);
        tvCurrentMusicInfo.setText(currentMusicInfo);
        tvCurrentPlayTime.setText(currentPlayTime);
        tvMediaTime.setText(mediaTime);
    }

    private void toPlayMusic(MediaFileBean mediaFileBean, Boolean isRestPlayer) {
        initServicePlayHelper();
        musicService.play(mediaFileBean, isRestPlayer, mPosition);
    }

    private void changePlayMode() {
        playMode++;
        if (playMode >= 3) {
            playMode = Constant.PLAY_MODE_LOOP;
        }
        if (playMode == Constant.PLAY_MODE_LOOP) {
            ivMediaLoop.setImageResource(R.mipmap.media_loop);
        } else if (playMode == Constant.PLAY_MODE_SHUFFLE) {
            ivMediaLoop.setImageResource(R.mipmap.media_shuffle);
        } else if (playMode == Constant.PLAY_MODE_SINGLE) {
            ivMediaLoop.setImageResource(R.mipmap.media_single);
        } else {
            ivMediaLoop.setImageResource(R.mipmap.media_loop);
            playMode = Constant.PLAY_MODE_LOOP;
        }
        //保存上次播放的播放模式
        DataRefreshService.setLastPlayMode(playMode);
        if (musicService != null) {
            musicService.setPlayMode(playMode);
        }
    }

    private void refreshListStatus() {
        DebugLog.debug("musicListMode " + musicListMode + "; position " + mPosition);
        for (BaseFragment fragment : viewPagerLists) {
            if (fragment.getListMode() == musicListMode) {
                fragment.setSelectPosition(mPosition);
                fragment.setSelection(mPosition);
            } else {
                fragment.setSelectPosition(-1);
            }
        }

    }

//    返回按钮事件
//    Button btnBack = (Button) view.findViewById(R.id.btnBack);
//        btnBack.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            getFragmentManager().popBackStack();
//        }
//    });


    /*
    * 现阶段是默认显示默认列表和收藏列表，播放列表需要重新定义,怎么显示才比较合理
    * 拓展-->后面是否支持创建列表？列表数量是否有限制，显示多少列表比较合理
    *
    * 对策一：播放页面中歌的列表-只显示当前列表，
    *       优点：不受列表数量影响，只加载当前播放的列表，逻辑简单，后期维护方便
    *       缺点：当前代码逻辑改动较大，ViewPager需要全部改掉
    *            一个Fragment难以满足各个列表的情况，如默认列表和收藏列表，UI不一样，Adapter也不一样
    *
    * 对策二：最多支持创建3/5个自定义列表，展示全部播放列表
    *       优点：改动简单，延续当前使用的ViewPager框架，直接增加业务逻辑即可
    *           从用户的交互更友好，不用频繁返回首页切换列表
    *       缺点：展示所有播放列表，就必须限制列表的个数，否则管理起来太复杂，要创建多个Fragment和Adapter
    *           后期维护成本很大--
    *
    * 最终决策：只显示当前列表，各个类型的列表共用一套Fragment和Adapter，根据列表类型自动适配Adapter和显示UI
    *       音乐列表的个数可以无限制，使用SQLite数据库框架，联表方式来存储
    *       列表单独放一个表，音乐信息放一个表（同一首歌可能存在于不同的列表中，所以会存储多条记录）
    *       查询的时候可以使用联表查询的方式来获取音乐
    * 播放列表的格式统一，采用默认列表的样式（即歌曲名后面带有一个删除的按钮，不带收藏按钮）
    * 收藏按钮放置播放页面中
    *
    * 最终对策(new)：显示三个列表--（当前播放列表、历史播放列表、上次播放列表），共用一套Fragment和Adapter
    *
    * */

}