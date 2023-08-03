package com.example.mediaplayproject.activity;

import androidx.annotation.NonNull;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.FavoriteMusicAdapter;
import com.example.mediaplayproject.adapter.MusicAdapter;
import com.example.mediaplayproject.base.BasicActivity;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicPlayerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: MusicPlayActivity
 * @Description: 音乐播放主界面
 * @Author: wm
 * @UpdateRemark: 更新内容
 * @Version: 1.0
 */
public class MusicPlayActivity extends BasicActivity {

    private ImageView ivMediaLoop, ivMediaPre, ivMediaPlay, ivMediaNext, ivMediaList, ivCloseListView, ivLocalList;
    private SeekBar sbVolume, sbProgress;
    private ListView mMusicListView, mFavoriteListView;
    private TextView tvCurrentMusicInfo, tvCurrentPlayTime, tvMediaTime, tvDefaultList, tvFavoriteList;
    private boolean isShowList = false, mRegistered = false, firstPlay = true, isInitPlayHelper = false;
    private Context mContext;
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
    private LinearLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;
    private AudioManager mAudioManager;
    private MusicAdapter musicAdapter;
    private FavoriteMusicAdapter favoriteListAdapter;
    private MusicBroadcastReceiver mMusicBroadcastReceiver;
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private final String VOLUME_CHANGE_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private final String VOLUME_MUTE = "android.media.STREAM_MUTE_CHANGED_ACTION";
    private final static int HANDLER_MESSAGE_REFRESH_VOLUME = 0;
    public static final int HANDLER_MESSAGE_REFRESH_PLAY_ICON = 1;
    public static final int HANDLER_MESSAGE_REFRESH_POSITION = 2;
    public static final int HANDLER_MESSAGE_TURN_TO_DEFAULT_LIST = 3;
    private int mPosition = 0;
    private MusicPlayService musicService;
    /**
     * playMode:播放模式 0->循环播放; 1->随机播放; 2->单曲播放;
     * 主要是控制播放上下曲的position
     */
    private int playMode = 0;

    /**
     * musicListMode:播放的来源 0->默认列表; 1->收藏列表; 后面可以扩展其他的列表
     */
    private int musicListMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        updateMusicList();
        initData();
        registerReceiver();
        createFloatView();
        //启动MusicPlayService服务
        Intent bindIntent = new Intent(MusicPlayActivity.this, MusicPlayService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initServicePlayHelper();
        if (musicService != null) {
            //再次进入界面时刷新播放状态按钮，初次进入默认为暂停状态
            ivMediaPlay.setImageResource(musicService.isPlaying() ? R.mipmap.media_pause : R.mipmap.media_play);
            //isInitPlayHelper：是否已经初始化; firstPlay :首次播放
            isInitPlayHelper = musicService.getInitResult();
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListMode = musicService.getMusicListMode();
        }
        //重新进入界面之后都获取一下音量信息和当前音乐列表
        initVolume();
//        DebugLog.debug("isInitPlayHelper " + isInitPlayHelper + "; firstPlay " + firstPlay + "; position " + mPosition);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
        // 解绑服务：注意bindService后 必须要解绑服务，否则会报-连接资源异常
        if (null != connection) {
            unbindService(connection);
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/4 15:02
     * @description 创建ServiceConnection
     */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugLog.debug("onServiceConnected");
            musicService = ((MusicPlayService.MyBinder) service).getService(mContext);
            if (musicService != null) {
                //service创建成功的时候立即初始化
                initServicePlayHelper();
                //获取到Service对象之后创建Adapter，避免后面打开列表时多次创建
                musicAdapter = new MusicAdapter(mContext, defaultList);
                favoriteListAdapter = new FavoriteMusicAdapter(mContext, favoriteList);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            DebugLog.debug("onServiceDisconnected");
        }
    };

    /**
     * @author wm
     * @createTime 2023/2/2 10:54
     * @description 从BasicApplication中获取音乐列表，上次播放的信息等
     */
    private void updateMusicList() {
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        playMode = DataRefreshService.getLastPlayMode();
        musicListMode = DataRefreshService.getLastPlayListMode();
        mPosition = DataRefreshService.getLastPosition();
    }

    /**
     * @author wm
     * @createTime 2023/2/9 17:59
     * @description 改变当前的列表
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

    /**
     * @author wm
     * @createTime 2023/2/2 10:59
     * @description 初始化音量
     */
    private void initVolume() {
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //当前设备的
        if (currentVolume >= 0 && currentVolume <= 150) {
            int afterVolume = (int) (currentVolume / 1.5);
            sbVolume.setProgress(afterVolume);
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/3 17:58
     * @description 获取组件、初始化数据
     */
    private void initData() {
        ivMediaLoop = findViewById(R.id.bt_loop);
        ivMediaPre = findViewById(R.id.bt_pre);
        ivMediaPlay = findViewById(R.id.bt_play);
        ivMediaNext = findViewById(R.id.bt_next);
        ivMediaList = findViewById(R.id.bt_list);
        sbVolume = findViewById(R.id.sb_volume);
        sbProgress = findViewById(R.id.sb_progress);
        tvCurrentMusicInfo = findViewById(R.id.tv_music_info);
        tvCurrentPlayTime = findViewById(R.id.tv_music_current_time);
        tvMediaTime = findViewById(R.id.tv_music_time);

        ivMediaLoop.setOnClickListener(mListener);
        ivMediaPre.setOnClickListener(mListener);
        ivMediaPlay.setOnClickListener(mListener);
        ivMediaNext.setOnClickListener(mListener);
        ivMediaList.setOnClickListener(mListener);
        sbVolume.setOnSeekBarChangeListener(mSeekBarListener);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        //首次进入app时先获取一次音乐列表信息的来源
        switchMusicList();

        //初始化播放主页的状态
        initPlayStateAndInfo();
    }

    /**
     * @author wm
     * @createTime 2023/2/13 14:56
     * @description 初始化播放主页各按钮的状态
     */
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

        //初始化播放模式的图标
        if (playMode == 0) {
            ivMediaLoop.setImageResource(R.mipmap.media_loop);
        } else if (playMode == 1) {
            ivMediaLoop.setImageResource(R.mipmap.media_shuffle);
        } else if (playMode == 2) {
            ivMediaLoop.setImageResource(R.mipmap.media_single);
        }
    }

    /**
     * @author wm
     * @createTime 2023/2/8 16:48
     * @description 初始化PlayHelper
     */
    private void initServicePlayHelper() {
        if (musicService != null) {
            //刷新Service里面的内容时，不用每次都初始化，最主要的是更新position和musicInfo
            //初始化的时候需要先调用initPlayData方法更新各项数据，避免数组越界
            musicService.initPlayData(musicInfo, mPosition, musicListMode, playMode);
            if (!isInitPlayHelper) {
                isInitPlayHelper = true;
                musicService.initPlayHelper(sbProgress, tvCurrentMusicInfo, tvCurrentPlayTime, tvMediaTime, handler);
            }
        }
    }


    private View.OnClickListener mListener = new View.OnClickListener() {
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
                toPlayMusic(musicInfo.get(mPosition), firstPlay, handler);
                firstPlay = false;
            } else if (view == ivMediaNext) {
                initServicePlayHelper();
                musicService.playNext();
            } else if (view == ivMediaList) {
                isShowList = true;
                showFloatView();
            } else if (view == ivCloseListView) {
                mWindowManager.removeView(mFloatLayout);
            } else if (view == ivLocalList) {
                initListHighLight();
            } else if (view == tvDefaultList) {
                clickTvDefaultList();
            } else if (view == tvFavoriteList) {
                clickFavoriteList();
            }
        }
    };

    /**
     * @author wm
     * @createTime 2023/2/13 15:01
     * @description 点击默认列表的操作
     */
    private void clickTvDefaultList() {
        initListHighLight();
        //这里的两个列表都是从BaseApplication中拿到，是同一对象，不需要再重复赋值
        musicAdapter.notifyDataSetChanged();
        mMusicListView.setVisibility(View.VISIBLE);
        mFavoriteListView.setVisibility(View.GONE);
        //这里直接使用setTextColor(R.color.white)是不会起作用的
        tvDefaultList.setTextColor(mContext.getResources().getColorStateList(R.color.text_pressed));
        tvFavoriteList.setTextColor(mContext.getResources().getColorStateList(R.color.white));
    }

    /**
     * @author wm
     * @createTime 2023/2/13 15:01
     * @description 点击收藏列表的操作
     */
    private void clickFavoriteList() {
        initListHighLight();
        favoriteListAdapter.notifyDataSetChanged();
        mMusicListView.setVisibility(View.GONE);
        mFavoriteListView.setVisibility(View.VISIBLE);
        tvFavoriteList.setTextColor(mContext.getResources().getColorStateList(R.color.text_pressed));
        tvDefaultList.setTextColor(mContext.getResources().getColorStateList(R.color.white));
    }

    /**
     * @author wm
     * @createTime 2023/2/4 15:09
     * @description 音量拖动条的监听处理
     */
    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
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

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

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
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wmParams.flags = flags;
        //调整悬浮窗显示的停靠位置为右侧底部
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 以屏幕右下角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;

    }

    private void showFloatView() {
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.layout_music_lit, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
        //初始化视窗
        initFloatView();
    }

    /**
     * @author wm
     * @createTime 2023/2/9 23:27
     * @description 初始化视窗、对视窗中的控件进行监听
     */
    @SuppressLint({"ResourceAsColor", "UseCompatLoadingForColorStateLists"})
    private void initFloatView() {

        //浮动窗口关闭按钮
        ivCloseListView = mFloatLayout.findViewById(R.id.iv_list_close);
        ivCloseListView.setOnClickListener(mListener);

        //定位当前播放歌曲
        ivLocalList = mFloatLayout.findViewById(R.id.iv_local_music);
        ivLocalList.setOnClickListener(mListener);

        //显示收藏列表
        tvFavoriteList = mFloatLayout.findViewById(R.id.tv_favorite_list);
        tvFavoriteList.setOnClickListener(mListener);

        //显示默认列表
        tvDefaultList = mFloatLayout.findViewById(R.id.tv_default_list);
        tvDefaultList.setOnClickListener(mListener);

        mMusicListView = mFloatLayout.findViewById(R.id.lv_musicList);
        mMusicListView.setAdapter(musicAdapter);
        mMusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPosition = position;
                musicListMode = 0;
                //一定要在mPosition重新赋值之后调用switchMusicList，因为该方法里面会更新service的position，避免数组越界
                switchMusicList();
                //点击歌曲播放
                toPlayMusic(defaultList.get(mPosition), true, handler);
                //若从列表点击播放，则暂停播放按钮就设置为非首次播放
                firstPlay = false;
                musicAdapter.setSelectPosition(mPosition);
            }
        });


        //喜欢的列表
        mFavoriteListView = mFloatLayout.findViewById(R.id.lv_favoriteList);
        mFavoriteListView.setAdapter(favoriteListAdapter);
        mFavoriteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPosition = position;
                musicListMode = 1;
                //一定要在mPosition重新赋值之后调用switchMusicList，因为该方法里面会更新service的position，避免数组越界
                switchMusicList();
                //点击歌曲播放
                toPlayMusic(favoriteList.get(mPosition), true, handler);
                //若从列表点击播放，则暂停播放按钮就设置为非首次播放
                firstPlay = false;
                favoriteListAdapter.setSelectPosition(mPosition);
            }
        });

        //更新listView的当前播放高亮效果
        initListHighLight();

        //根据当前播放的来源，打开列表时显示相应的列表页面
        if (musicListMode == 0) {
            mMusicListView.setVisibility(View.VISIBLE);
            mFavoriteListView.setVisibility(View.GONE);
            tvDefaultList.setTextColor(this.getResources().getColorStateList(R.color.text_pressed));
        } else if (musicListMode == 1) {
            mFavoriteListView.setVisibility(View.VISIBLE);
            mMusicListView.setVisibility(View.GONE);
            tvFavoriteList.setTextColor(this.getResources().getColorStateList(R.color.text_pressed));
        }

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
     * @author wm
     * @createTime 2023/2/11 11:03
     * @description 刷新ListView的当前高亮效果
     */
    private void initListHighLight() {
        if (musicListMode == 0) {
            //为音乐列表添加高亮处理（当前播放和选中的选项都会高亮）
            musicAdapter.setSelectPosition(mPosition);
            //在列表中将当前播放的歌曲显示在列表顶部（整体显示在顶部，顺序未改变）
            mMusicListView.setSelection(mPosition);
            //取消另一个列表的当前播放高亮效果
            favoriteListAdapter.setSelectPosition(-1);
        } else if (musicListMode == 1) {
            //若移除了收藏歌曲（下标在当前播放之前），列表整体前移，当前的下标应该改变了，这里必须拿到最新的position值
            //为音乐列表添加高亮处理（当前播放和选中的选项都会高亮）
            favoriteListAdapter.setSelectPosition(mPosition);
            //在列表中将当前播放的歌曲显示在列表顶部（整体显示在顶部，顺序未改变）
            mFavoriteListView.setSelection(mPosition);
            //取消另一个列表的当前播放高亮效果
            musicAdapter.setSelectPosition(-1);
        }
    }


    /**
     * @author wm
     * @createTime 2023/2/8 16:55
     * @description 处理音乐的播放和暂停等
     */
    private void toPlayMusic(MediaFileBean mediaFileBean, Boolean isRestPlayer, Handler handler) {
        initServicePlayHelper();
        musicService.play(mediaFileBean, isRestPlayer, handler, mPosition);
    }

    /**
     * @author wm
     * @createTime 2023/2/8 18:12
     * @description 更改播放模式；0->循环播放; 1->随机播放; 2->单曲播放;
     */
    private void changePlayMode() {
        playMode++;
        if (playMode >= 3) {
            playMode = 0;
        }
        if (playMode == 0) {
            ivMediaLoop.setImageResource(R.mipmap.media_loop);
        } else if (playMode == 1) {
            ivMediaLoop.setImageResource(R.mipmap.media_shuffle);
        } else if (playMode == 2) {
            ivMediaLoop.setImageResource(R.mipmap.media_single);
        } else {
            ivMediaLoop.setImageResource(R.mipmap.media_loop);
            playMode = 0;
        }
        //保存上次播放的播放模式
        DataRefreshService.setLastPlayMode(playMode);
        if (musicService != null) {
            musicService.setPlayMode(playMode);
        }
    }

    /**
     * @param
     * @author wm
     * @createTime 2023/2/8 16:55
     * @description 注册音量广播接收器
     */
    public void registerReceiver() {
        mMusicBroadcastReceiver = new MusicBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VOLUME_CHANGE_ACTION);
        filter.addAction(VOLUME_MUTE);
        filter.addAction(MusicPlayService.DELETE_MUSIC_ACTION);
        mContext.registerReceiver(mMusicBroadcastReceiver, filter);
        mRegistered = true;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_music_play;
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
                msg.what = HANDLER_MESSAGE_REFRESH_VOLUME;
                Bundle bundle = new Bundle();
                bundle.putInt("volume", changedVolume);
                msg.setData(bundle);
                handler.sendMessage(msg);
            } else if (MusicPlayService.DELETE_MUSIC_ACTION.equals(intent.getAction())) {
                //删除收藏歌曲（删除的下标小于当前播放的下标）的时候，需要刷新收藏列表的高亮下标
                //若是在收藏列表界面，需要实时更新；
                //在默认界面切换过去的时候，会重新设定高亮的位置为position，所以需要拿到收藏列表里最新的position
                int deletePosition = intent.getExtras().getInt("musicPosition");
                if (musicListMode == 1) {
                    int isRefreshResult = favoriteListAdapter.checkRefreshPosition(deletePosition);
                    DebugLog.debug("isRefreshResult " + isRefreshResult);
                    if (isRefreshResult == 1) {
                        //删除的是小于当前播放下标的歌曲
                        favoriteListAdapter.refreshSelectionPosition();
                        mPosition--;
                    } else if (isRefreshResult == 0) {
                        //删除的是当前的歌曲，需要先隐藏高亮坐标,等收到service的消息后再打开
                        favoriteListAdapter.setSelectPosition(-1);
                        favoriteListAdapter.notifyDataSetChanged();
                    }

                    //同步更新Service中的mPosition值
                    musicService.setPosition(mPosition);
                }
            }
        }
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


    /**
     * @author wm
     * @createTime 2023/2/3 18:22
     * @description 创建handler，用于更新UI
     */
    final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("ResourceAsColor")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_REFRESH_VOLUME) {
                //媒体音量发生变化--更新音量条
                int volume = msg.getData().getInt("volume");
                if (volume >= 0 && volume <= 150) {
                    int afterVolume = (int) (volume / 1.5);
                    sbVolume.setProgress(afterVolume);
                }
            } else if (msg.what == HANDLER_MESSAGE_REFRESH_PLAY_ICON) {
                //service发送的信息，用于更新播放状态的图标
                boolean isPlaying = msg.getData().getBoolean("iconType");
//                DebugLog.debug("refresh play icon , change playing icon " + isPlaying);
                ivMediaPlay.setImageResource(isPlaying ? R.mipmap.media_pause : R.mipmap.media_play);
                //接收到service发送的播放状态改变之后，刷新Activity的值（针对未刷新页面的情况）
                firstPlay = false;
                isInitPlayHelper = true;
                mPosition = musicService.getPosition();
            } else if (msg.what == HANDLER_MESSAGE_REFRESH_POSITION) {
                //service发送的信息，用于删除歌曲后自动播放下一曲或者在通知栏切歌后的mPosition
                int newPosition = msg.getData().getInt("newPosition");
                DebugLog.debug("after delete new position " + newPosition);
                if (musicListMode == 0) {
                    musicAdapter.setSelectPosition(newPosition);
                    musicAdapter.notifyDataSetChanged();
                } else if (musicListMode == 1) {
                    favoriteListAdapter.setSelectPosition(newPosition);
                    favoriteListAdapter.notifyDataSetChanged();
                }
            } else if (msg.what == HANDLER_MESSAGE_TURN_TO_DEFAULT_LIST) {
                DebugLog.debug("HANDLER_MESSAGE_TURN_TO_DEFAULT_LIST");
                //service发送的信息，收藏列表清空之后，直接切换为默认列表
                musicListMode = 0;
                mPosition = 0;
                firstPlay = true;
                isInitPlayHelper = false;
                switchMusicList();
                //UI上跳转到默认列表，直接调用点击事件的处理逻辑
                clickTvDefaultList();
                //UI上刷新播放信息和播放状态
                initPlayStateAndInfo();
                //刷新播放按钮
                ivMediaPlay.setImageResource(R.mipmap.media_play);
                //刷新通知栏的播放按钮状态
                musicService.updateNotificationShow(0, false);
                //保存最后播放歌曲的id--直接保存
                DataRefreshService.setLastMusicId(defaultList.get(mPosition).getId());
            }
        }
    };

}