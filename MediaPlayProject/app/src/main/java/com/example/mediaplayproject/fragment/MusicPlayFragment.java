package com.example.mediaplayproject.fragment;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mediaplayproject.R;
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

    private final Context mContext;
    private View playView;
    private ImageView ivMediaLoop, ivMediaPre, ivMediaPlay, ivMediaNext, ivMediaList, ivMediaLike;
    private SeekBar sbVolume, sbProgress;
    private TextView tvCurrentMusicInfo, tvCurrentPlayTime, tvMediaTime;
    private String currentTime = "00:00";
    private boolean mRegistered = false, firstPlay = true, isPlaying = false;
    private AudioManager mAudioManager;
    private Handler mActivityHandle;

    private MusicBroadcastReceiver mMusicBroadcastReceiver;
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private final String VOLUME_CHANGE_ACTION = "android.media.VOLUME_CHANGED_ACTION";

    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private int mPosition = 0;
    private int playMode = 0;
    private String musicListName = Constant.LIST_MODE_DEFAULT_NAME;


    /**
     * MusicPlayService对象，控制音乐播放service类
     */
    private MusicPlayService musicService;

    public MusicPlayFragment(Context context) {
        mContext = context;
    }

    @SuppressLint("StaticFieldLeak")
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
        DebugLog.debug("");
    }

    public void setDataFromMainActivity(MusicPlayService service, Handler handler, String listName, int position) {
        DebugLog.debug("");
        this.mActivityHandle = handler;
        musicService = service;
        musicListName = listName;
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
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    private void initMusicSource() {
        // 从BasicApplication中获取音乐列表，上次播放的信息等
        playMode = DataRefreshService.getLastPlayMode();
        musicListName = DataRefreshService.getLastPlayListName();
        mPosition = DataRefreshService.getLastPosition();
    }

    private void bindView() {
        ivMediaLoop = playView.findViewById(R.id.iv_loop);
        ivMediaPre = playView.findViewById(R.id.iv_pre);
        ivMediaPlay = playView.findViewById(R.id.iv_play);
        ivMediaNext = playView.findViewById(R.id.iv_next);
        ivMediaList = playView.findViewById(R.id.iv_list);
        ivMediaLike = playView.findViewById(R.id.iv_like);
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
        ivMediaLike.setOnClickListener(mListener);
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
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListName = musicService.getMusicListName();
        }
    }

    private void switchMusicList() {
        List<MediaFileBean> tempList = DataRefreshService.getMusicListByName(musicListName);
        if (tempList != null && tempList.size() > 0){
            // 更新播放列表等数据
            musicInfo = tempList;
        }

        initServicePlayHelper();
    }

    @SuppressLint("SetTextI18n")
    private void initPlayStateAndInfo() {
        //获取音乐列表之后，若是列表为不空，则将当前下标的歌曲信息显示出来
        if (musicInfo.size() > 0) {
            tvCurrentMusicInfo.setText(musicInfo.get(mPosition).getTitle());
            tvCurrentPlayTime.setText(currentTime);
            tvMediaTime.setText(MusicPlayerHelper.formatTime(musicInfo.get(mPosition).getDuration()));
            ivMediaPre.setEnabled(true);
            ivMediaPlay.setEnabled(true);
            ivMediaNext.setEnabled(true);
            boolean isLike = musicInfo.get(mPosition).isLike();
            ivMediaLike.setImageResource(isLike ? R.mipmap.ic_list_like_choose : R.mipmap.ic_list_like);
            ivMediaLike.setEnabled(true);
        } else {
            //若列表为空，则播放、上下曲都不可点击
            tvCurrentMusicInfo.setText("");
            tvCurrentPlayTime.setText("");
            tvMediaTime.setText("");
            ivMediaPre.setEnabled(false);
            ivMediaPlay.setEnabled(false);
            ivMediaNext.setEnabled(false);
            ivMediaLike.setEnabled(false);
            currentTime = "00:00";
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
            musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);
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
                // 判断当前是否是首次播放，若是首次播放，则需要设置重头开始播放（Media的首次播放需要reset等流程）
                toPlayMusic(musicInfo.get(mPosition), firstPlay);
                firstPlay = false;
            } else if (view == ivMediaNext) {
                initServicePlayHelper();
                musicService.playNext();
            } else if (view == ivMediaList) {
                Message msg = new Message();
                msg.what = Constant.HANDLER_MESSAGE_SHOW_LIST_FRAGMENT;
                mActivityHandle.sendMessage(msg);
            } else if (view == ivMediaLike) {
                boolean isLike = musicInfo.get(mPosition).isLike();
                ivMediaLike.setImageResource(!isLike ? R.mipmap.ic_list_like_choose : R.mipmap.ic_list_like);
                musicInfo.get(mPosition).setLike(!isLike);
                if (!isLike){
                    // 加入收藏
                    DataRefreshService.addMusicToFavoriteList(musicInfo.get(mPosition));
                } else {
                    // 取消收藏
                    DataRefreshService.removeFavoriteMusic(musicInfo.get(mPosition));
                }
                // 发送消息给Activity更新列表状态
                Message msg = new Message();
                msg.what = Constant.HANDLER_MESSAGE_REFRESH_LIST_STATE;
                mActivityHandle.sendMessage(msg);
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
     * @createTime 2023/2/8 16:55
     * @description 注册音量广播接收器
     */
    public void registerReceiver() {
        mMusicBroadcastReceiver = new MusicBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VOLUME_CHANGE_ACTION);
        filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
        mContext.registerReceiver(mMusicBroadcastReceiver, filter);
        mRegistered = true;
    }

    /**
     *  广播接收器，接收音量(媒体音量)改变的广播
     *  @author wm
     *  @createTime 2023/9/3 15:18
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
            }
        }
    };

    /**
     *  更新播放状态
     *  @author wm
     *  @createTime 2023/9/3 16:23
     *  @param state: 是否正在播放：true-播放；false-暂停
     *  @param newPosition:播放歌曲的下标
     *  @param musicListName:播放歌曲所属的列表名
     *  @param musicInfo:播放歌曲所属列表
     */
    public void refreshPlayState(boolean state, int newPosition, String musicListName, List<MediaFileBean> musicInfo) {
        DebugLog.debug("refresh play state ");
        // 更新播放状态和收藏状态
        isPlaying = state;
        mPosition = newPosition;
        this.musicListName = musicListName;
        this.musicInfo = musicInfo;
        initPlayStateAndInfo();
    }

    /**
     *  更新播放进度
     *  @author wm
     *  @createTime 2023/9/3 16:23
     *  @param seekbarProgress: 歌曲进度
     *  @param currentMusicInfo: 当前播放歌曲名
     *  @param currentPlayTime: 当前播放时间
     *  @param mediaTime: 歌曲总时间
     */
    public void refreshCurrentPlayInfo(int seekbarProgress, String currentMusicInfo, String currentPlayTime, String mediaTime) {
        // 刷新播放进度及时间
        sbProgress.setProgress(seekbarProgress);
        tvCurrentMusicInfo.setText(currentMusicInfo);
        currentTime = currentPlayTime;
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
        DataRefreshService.setLastPlayInfo(musicListName,mPosition,musicInfo.get(mPosition).getId(),playMode);
        if (musicService != null) {
            musicService.setPlayMode(playMode);
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


}