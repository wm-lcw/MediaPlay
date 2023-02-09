package com.example.mediaplayproject.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.example.mediaplayproject.adapter.MusicAdapter;
import com.example.mediaplayproject.base.BasicActivity;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicPlayerHelper;
import com.example.mediaplayproject.utils.SearchFiles;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: MusicPlayActivity
 * @Description: 音乐播放主界面
 * @Author: wm
 * @CreateDate: 2023/2/2
 * @UpdateUser: updater
 * @UpdateDate: 2023/2/2
 * @UpdateRemark: 更新内容
 * @Version: 1.0
 */
public class MusicPlayActivity extends BasicActivity {
    private ImageView ivMediaLoop, ivMediaPre, ivMediaPlay, ivMediaNext, ivMediaList, ivCloseListView, ivLocalList;
    private SeekBar sbVolume, sbProgress;
    private ListView mMusicListView;
    private TextView tvCurrentMusicInfo, tvCurrentPlayTime, tvMediaTime;
    private boolean isShowList = false, mRegistered = false, firstPlay = true, isInitPlayHelper = false;
    private Context mContext;
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private LinearLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;
    private AudioManager mAudioManager;
    private MusicAdapter musicAdapter;
    private MusicBroadcastReceiver mMusicBroadcastReceiver;
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private final String VOLUME_CHANGE_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private final String VOLUME_MUTE = "android.media.STREAM_MUTE_CHANGED_ACTION";
    private final static int HANDLER_MESSAGE_REFRESH_VOLUME = 0;
    public static final int HANDLER_MESSAGE_REFRESH_PLAY_ICON = 1;
    private int mPosition = 0;
    private MusicPlayService musicService;
    /**
     * playMode:播放模式 0->循环播放; 1->随机播放; 2->单曲播放;
     * 主要是控制播放上下曲的position
     */
    private int playMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        updateMusicFiles();
        initData();
        registerReceiver();
        //启动MusicPlayService服务
        Intent bindIntent = new Intent(MusicPlayActivity.this, MusicPlayService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initPlayHelper();
        if (musicService != null) {
            //再次进入界面时刷新播放状态按钮，初次进入默认为暂停状态
            ivMediaPlay.setImageResource(musicService.isPlaying() ? R.drawable.set_media_pause_style : R.drawable.set_media_play_style);
            //isInitPlayHelper：是否已经初始化; firstPlay :首次播放
            isInitPlayHelper = musicService.getInitResult();
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
        }
        initVolume();
        //检查是否是首次播放歌曲
        DebugLog.debug("isInitPlayHelper " + isInitPlayHelper + "; firstPlay " + firstPlay + "; position " + mPosition);
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
     * @version V1.0
     * @Title
     * @author wm
     * @createTime 2023/2/4 15:02
     * @description 创建ServiceConnection
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugLog.debug("onServiceConnected");
            musicService = ((MusicPlayService.MyBinder) service).getService(mContext);
            if (musicService != null) {
                //service创建成功的时候立即初始化
                initPlayHelper();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            DebugLog.debug("onServiceDisconnected");
        }
    };

    /**
     * @version V1.0
     * @Title updateMusicFiles
     * @author wm
     * @createTime 2023/2/2 10:54
     * @description 获取音频文件
     */
    private void updateMusicFiles() {
        SearchFiles mSearcherFiles = SearchFiles.getInstance(mContext);
        musicInfo = mSearcherFiles.getMusicInfo();
        //打印输出音乐列表
//        if (musicInfo.size() > 0) {
//            Iterator<MediaFileBean> iterator = musicInfo.iterator();
//            while (iterator.hasNext()) {
//                MediaFileBean mediaFileBean = iterator.next();
//                DebugLog.debug(mediaFileBean.getTitle());
//            }
//        }
    }

    /**
     * @version V1.0
     * @Title initVolume
     * @author wm
     * @createTime 2023/2/2 10:59
     * @description 初始化音量
     */
    private void initVolume() {
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        DebugLog.debug("getCurrentVolume " + currentVolume);
        //当前设备的
        if (currentVolume >= 0 && currentVolume <= 150) {
            int afterVolume = (int) (currentVolume / 1.5);
            sbVolume.setProgress(afterVolume);
        }
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title initData
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
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title initPlayHelper
     * @author wm
     * @createTime 2023/2/8 16:48
     * @description 初始化PlayHelper
     */
    private void initPlayHelper() {
        DebugLog.debug("");
        if (musicService != null && !isInitPlayHelper) {
            isInitPlayHelper = true;
            musicService.initPlayHelper(sbProgress, tvCurrentMusicInfo, tvCurrentPlayTime, tvMediaTime, musicInfo, handler);
        }
    }


    private View.OnClickListener mListener = new View.OnClickListener() {
        @SuppressLint("ResourceType")
        @Override
        public void onClick(View view) {
            if (view == ivMediaLoop) {
                changePlayMode();
            } else if (view == ivMediaPre) {
                initPlayHelper();
                musicService.playPre();
            } else if (view == ivMediaPlay) {
                DebugLog.debug("current position " + mPosition);
                // 判断当前是否是首次播放，若是首次播放，则需要设置重头开始播放（Media的首次播放需要reset等流程）
                toPlayMusic(musicInfo.get(mPosition), firstPlay, handler);
                firstPlay = false;
            } else if (view == ivMediaNext) {
                initPlayHelper();
                musicService.playNext();
            } else if (view == ivMediaList) {
                isShowList = true;
                createFloatView();
            }
        }
    };

    /**
     * @version V1.0
     * @Title
     * @author wm
     * @createTime 2023/2/4 15:09
     * @description 音量拖动条的监听处理
     * @param
     * @return
     */
    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == sbVolume) {
                DebugLog.debug("volume progress is " + progress);
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
     * @param
     * @return
     * @version V1.0
     * @Title createFloatView
     * @author wm
     * @createTime 2023/2/3 18:19
     * @description 创建悬浮窗
     */
    private void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        DebugLog.debug("mWindowManager--->" + mWindowManager);
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

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.layout_music_lit, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);

        //浮动窗口关闭按钮
        ivCloseListView = mFloatLayout.findViewById(R.id.iv_list_close);
        ivCloseListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWindowManager.removeView(mFloatLayout);
            }
        });
        //定位当前播放歌曲
        ivLocalList = mFloatLayout.findViewById(R.id.iv_local_music);
        ivLocalList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMusicListView.setSelection(mPosition);
            }
        });

        mMusicListView = mFloatLayout.findViewById(R.id.lv_musicList);
        musicAdapter = new MusicAdapter(mContext, musicInfo);
        mMusicListView.setAdapter(musicAdapter);
        mMusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPosition = position;
                //点击歌曲播放
                toPlayMusic(musicInfo.get(mPosition), true, handler);
                //若从列表点击播放，则暂停播放按钮就设置为非首次播放
                firstPlay = false;
                musicAdapter.setSelectPosition(mPosition);
            }
        });
        //为音乐列表添加高亮处理（当前播放和选中的选项都会高亮）
        musicAdapter.setSelectPosition(mPosition);
        //在列表中将当前播放的歌曲显示在列表顶部（整体显示在顶部，顺序未改变）
        mMusicListView.setSelection(mPosition);


        //监听返回键--隐藏悬浮窗口
//        mFloatLayout.setOnKeyListener(new View.OnKeyListener() {
//            @Override
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                switch (keyCode) {
//                    case KeyEvent.KEYCODE_BACK:
//                        mWindowManager.removeView(mFloatLayout);
//                        return true;
//                    default:
//                        return false;
//                }
//            }
//        });

        /* 点击窗口外部区域可消除
         将悬浮窗设置为全屏大小，外层有个透明背景，中间一部分视为内容区域,
         所以点击内容区域外部视为点击悬浮窗外部
         其中popupWindowView为全屏，listWindow为列表区域，触摸点没有落在列表区域，则隐藏列表*/
        final View popupWindowView = mFloatLayout.findViewById(R.id.ll_popup_window);
        final View listWindow = mFloatLayout.findViewById(R.id.ll_listWindow);
        popupWindowView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                Rect rect = new Rect();
                listWindow.getGlobalVisibleRect(rect);
                if (!rect.contains(x, y) && isShowList) {
                    mWindowManager.removeView(mFloatLayout);
                }
                DebugLog.debug("onTouch : " + x + ", " + y + ", rect: " + rect);
                return false;
            }
        });

    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title toPlayMusic
     * @author wm
     * @createTime 2023/2/8 16:55
     * @description 处理音乐的播放和暂停等
     */
    private void toPlayMusic(MediaFileBean mediaFileBean, Boolean isRestPlayer, Handler handler) {
        DebugLog.debug("play isResetPlay " + isRestPlayer);
        initPlayHelper();
        musicService.play(mediaFileBean, isRestPlayer, handler, mPosition);
    }

    /**
     * @version V1.0
     * @Title changePlayMode
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
            ivMediaLoop.setImageResource(R.drawable.set_media_loop_mode_style);
        } else if (playMode == 1) {
            ivMediaLoop.setImageResource(R.drawable.set_media_random_mode_style);
        } else if (playMode == 2) {
            ivMediaLoop.setImageResource(R.drawable.set_media_single_mode_style);
        } else {
            ivMediaLoop.setImageResource(R.drawable.set_media_loop_mode_style);
            playMode = 0;
        }
        if (musicService != null) {
            musicService.setPlayMode(playMode);
        }
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title registerReceiver
     * @author wm
     * @createTime 2023/2/8 16:55
     * @description 注册音量广播接收器
     */
    public void registerReceiver() {
        mMusicBroadcastReceiver = new MusicBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VOLUME_CHANGE_ACTION);
        filter.addAction(VOLUME_MUTE);
        mContext.registerReceiver(mMusicBroadcastReceiver, filter);
        mRegistered = true;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_music_play;
    }

    /**
     * @param
     * @author wm
     * @version V1.0
     * @Title
     * @createTime 2023/2/3 18:21
     * @description 创建广播接收器，接收音量(媒体音量)改变的广播
     * @return
     */
    private class MusicBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            DebugLog.debug("receiver action " + intent.getAction());
            //媒体音量改变才通知
            if (VOLUME_CHANGE_ACTION.equals(intent.getAction())
                    && (intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC)) {
                int changedVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                DebugLog.debug("get change volume " + changedVolume);
                Message msg = new Message();
                msg.what = HANDLER_MESSAGE_REFRESH_VOLUME;
                Bundle bundle = new Bundle();
                bundle.putInt("volume", changedVolume);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }
    }

    /**
     *
     */
    /**
     * @param
     * @return
     * @version V1.0
     * @Title unregisterReceiver
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
     * @version V1.0
     * @Title
     * @author wm
     * @createTime 2023/2/3 18:22
     * @description 创建handler，用于更新UI
     * @param
     * @return
     */
    final Handler handler = new Handler(Looper.myLooper()) {
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
                DebugLog.debug("refresh play icon , change playing icon " + isPlaying);
                ivMediaPlay.setImageResource(isPlaying ? R.drawable.set_media_pause_style : R.drawable.set_media_play_style);
                //接收到service发送的播放状态改变之后，刷新Activity的值（针对未刷新页面的情况）
                firstPlay = false;
                isInitPlayHelper = true;
                mPosition = musicService.getPosition();
            }
        }
    };

}