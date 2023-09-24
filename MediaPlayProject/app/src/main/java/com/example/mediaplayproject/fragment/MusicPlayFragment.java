package com.example.mediaplayproject.fragment;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * @author wm
 */
public class MusicPlayFragment extends Fragment {

    private final Context mContext;
    private View playView;
    private ImageView ivBack, ivMore, ivMute, ivMediaLoop, ivMediaPre, ivMediaPlay, ivMediaNext, ivMediaList, ivMediaLike, ivMusicPic;
    private SeekBar sbVolume, sbProgress;
    private int maxVolume = 150;
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
    private MusicPlayService musicService;
    private Animation animation;

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
        DebugLog.debug("");
        bindView();
        registerReceiver();
    }

    /**
     *  从MainActivity更新数据到MusicPlayFragment中
     *  @author wm
     *  @createTime 2023/9/17 13:11
     * @param service:
     * @param handler:
     * @param listName:
     * @param position:
     */
    public void setDataFromMainActivity(MusicPlayService service, Handler handler, String listName, int position) {
        this.mActivityHandle = handler;
        musicService = service;
        musicListName = listName;
        mPosition = position;
    }

    @Override
    public void onResume() {
        super.onResume();
        DebugLog.debug("");
        initData();
        initPlayStateAndInfo();
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

    /**
     *  绑定控件
     *  @author wm
     *  @createTime 2023/9/17 13:12
     */
    private void bindView() {
        ivBack = playView.findViewById(R.id.iv_play_view_back);
        ivMore = playView.findViewById(R.id.iv_play_view_more);
        ivMute = playView.findViewById(R.id.iv_mute);
        ivMusicPic = playView.findViewById(R.id.iv_music_pic);
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

        ivBack.setOnClickListener(mListener);
        ivMore.setOnClickListener(mListener);
        ivMute.setOnClickListener(mListener);
        ivMediaLoop.setOnClickListener(mListener);
        ivMediaPre.setOnClickListener(mListener);
        ivMediaPlay.setOnClickListener(mListener);
        ivMediaNext.setOnClickListener(mListener);
        ivMediaList.setOnClickListener(mListener);
        ivMediaLike.setOnClickListener(mListener);
        sbVolume.setOnSeekBarChangeListener(mSeekBarListener);
        sbProgress.setOnSeekBarChangeListener(mSeekBarListener);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        // 设置图标旋转的动画
        animation = AnimationUtils.loadAnimation(mContext, R.anim.ic_playing_animation);
        //设置动画匀速运动
        // setInterpolator表示设置旋转速率。
        // LinearInterpolator为匀速效果，AccelerateInterpolator为加速效果，DecelerateInterpolator为减速效果
        LinearInterpolator lin = new LinearInterpolator();
        animation.setInterpolator(lin);

    }

    /**
     *  从DataRefreshService和service中获取数据
     *  @author wm
     *  @createTime 2023/9/17 13:13
     */
    private void initData() {
        playMode = DataRefreshService.getLastPlayMode();
        musicListName = DataRefreshService.getLastPlayListName();
        mPosition = DataRefreshService.getLastPosition();
        if (musicService != null) {
            //再次进入界面时刷新播放状态按钮，初次进入默认为暂停状态
            isPlaying = musicService.isPlaying();
            firstPlay = musicService.getFirstPlay();
            mPosition = musicService.getPosition();
            playMode = musicService.getPlayMode();
            musicListName = musicService.getMusicListName();
        }
        // 刷新音乐列表的来源
        List<MediaFileBean> tempList = DataRefreshService.getMusicListByName(musicListName);
        if (tempList != null && tempList.size() > 0){
            musicInfo = tempList;
        }
        initServicePlayHelper();
    }

    /**
     *  初始化播放状态
     *  @author wm
     *  @createTime 2023/9/17 13:15
     */
    @SuppressLint("SetTextI18n")
    private void initPlayStateAndInfo() {
        //获取音乐列表之后，若是列表为不空，则将当前下标的歌曲信息显示出来
        if (musicInfo.size() > 0) {
            tvCurrentMusicInfo.setText(musicInfo.get(mPosition).getTitle());
            tvCurrentPlayTime.setText(currentTime);
            tvMediaTime.setText(ToolsUtils.getInstance().formatTime(musicInfo.get(mPosition).getDuration()));
            ivMediaPre.setEnabled(true);
            ivMediaPlay.setEnabled(true);
            ivMediaNext.setEnabled(true);
            boolean isLike = musicInfo.get(mPosition).isLike();
            ivMediaLike.setImageResource(isLike ? R.mipmap.ic_list_like_choose : R.mipmap.ic_list_like);
            ivMediaLike.setEnabled(true);
        } else {
            //若列表为空，则播放、上下曲、收藏按钮都不可点击
            tvCurrentMusicInfo.setText("");
            tvCurrentPlayTime.setText("");
            tvMediaTime.setText("");
            ivMediaPre.setEnabled(false);
            ivMediaPlay.setEnabled(false);
            ivMediaNext.setEnabled(false);
            ivMediaLike.setEnabled(false);
            currentTime = "00:00";
        }
        // 设定音乐专辑图片
        ivMusicPic.setImageBitmap(getAlbumPicture(musicInfo.get(mPosition).getData()));

        ivMediaPlay.setImageResource(isPlaying ? R.mipmap.media_pause : R.mipmap.media_play);
        // 设置旋转图标的状态
        if (isPlaying){
            ivMusicPic.startAnimation(animation);
        } else {
            ivMusicPic.clearAnimation();
        }

        // 初始化播放模式的图标
        if (playMode == Constant.PLAY_MODE_SHUFFLE) {
            ivMediaLoop.setImageResource(R.mipmap.media_shuffle);
        } else if (playMode == Constant.PLAY_MODE_SINGLE) {
            ivMediaLoop.setImageResource(R.mipmap.media_single);
        } else {
            playMode = Constant.PLAY_MODE_LOOP;
            ivMediaLoop.setImageResource(R.mipmap.media_loop);
        }
    }

    /**
     *  获取歌曲专辑图片
     *  @author wm
     *  @createTime 2023/9/24 19:40
     * @param dataPath: 音乐资源的路径
     * @return : android.graphics.Bitmap
     */
    public Bitmap getAlbumPicture(String dataPath) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(dataPath);
        byte[] data = mmr.getEmbeddedPicture();
        Bitmap albumPicture = null;
        if (data != null) {
            // 获取bitmap对象
            albumPicture = BitmapFactory.decodeByteArray(data, 0, data.length);
            // 获取宽高
            int width = albumPicture.getWidth();
            int height = albumPicture.getHeight();
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) 120 / width);
            float sy = ((float) 120 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
        } else {
            albumPicture = BitmapFactory.decodeResource(getResources(), R.drawable.music);
            //music1是从歌曲文件读取不出来专辑图片时用来代替的默认专辑图片
            int width = albumPicture.getWidth();
            int height = albumPicture.getHeight();
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) 120 / width);
            float sy = ((float) 120 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
        }
        return albumPicture;
    }


    /**
     *  初始化音量和音量条
     *  @author wm
     *  @createTime 2023/9/17 13:08
     */
    private void initVolume() {
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        // 获取当前设备媒体音量的最大值
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 设置拖动条的最大值,与设备的音量最大值保持同步，不需要做数值转换
        sbVolume.setMax(maxVolume);
        if (currentVolume >= 0 && currentVolume <= maxVolume) {
            sbVolume.setProgress(currentVolume);
        }
    }

    /**
     *  初始化service
     *  执行播放之前需要先调用initPlayData方法更新各项数据，避免数组越界
     *  @author wm
     *  @createTime 2023/9/17 13:26
     */
    private void initServicePlayHelper() {
        if (musicService != null) {
            musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);
        }
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @SuppressLint({"ResourceType", "UseCompatLoadingForColorStateLists", "UseCompatLoadingForDrawables"})
        @Override
        public void onClick(View view) {
            if (view == ivBack) {
                // 返回主页
                Intent intent = new Intent(Constant.RETURN_MAIN_VIEW_ACTION);
                mContext.sendBroadcast(intent);
            } else if (view == ivMore) {
                DebugLog.debug("more");
            } else if (view == ivMute) {
                int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (currentVolume > 0){
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.FLAG_PLAY_SOUND);
                     Drawable volumeDrawable = mContext.getResources().getDrawable(R.drawable.volume_mute2_style, null);
                    ivMute.setBackground(volumeDrawable);
                } else {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,20,AudioManager.FLAG_PLAY_SOUND);
                    Drawable volumeDrawable = mContext.getResources().getDrawable(R.drawable.volume_mute1_style, null);
                    ivMute.setBackground(volumeDrawable);
                }
            } else if (view == ivMediaLoop) {
                changePlayMode();
            } else if (view == ivMediaPre) {
                initServicePlayHelper();
                musicService.playPre();
            } else if (view == ivMediaPlay) {
                initServicePlayHelper();
                // 判断当前是否是首次播放，若是首次播放，则需要设置重头开始播放（Media的首次播放需要reset等流程）
                musicService.play(musicInfo.get(mPosition), firstPlay, mPosition);
                firstPlay = false;
            } else if (view == ivMediaNext) {
                initServicePlayHelper();
                musicService.playNext();
            } else if (view == ivMediaList) {
                // 打开列表悬浮窗，需要发送消息给MainActivity处理
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
                    // 取消收藏，需要传递当前的列表名去判断是否要刷新播放状态
                    DataRefreshService.removeFavoriteMusic(musicListName, musicInfo.get(mPosition));
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
                if (fromUser) {
                    // 手动拖动seekbar时才设置音量（排除外部改变音量的影响）
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND);
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
                if (volume >= 0 && volume <= maxVolume) {
                    sbVolume.setProgress(volume);
                }
            }
        }
    };

    /**
     *  更新播放状态，由MainActivity调用
     *  @author wm
     *  @createTime 2023/9/3 16:23
     * @param state : 是否正在播放：true-播放；false-暂停
     * @param newPosition :播放歌曲的下标
     * @param musicListName :播放歌曲所属的列表名
     * @param musicInfo :播放歌曲所属列表
     * @param firstPlay :标识是否是首次播放
     */
    public void refreshPlayState(boolean state, int newPosition, String musicListName, List<MediaFileBean> musicInfo, boolean firstPlay) {
        // 更新播放状态和收藏状态
        isPlaying = state;
        mPosition = newPosition;
        this.musicListName = musicListName;
        this.musicInfo = musicInfo;
        this.firstPlay = firstPlay;
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

    /**
     *  切换播放模式
     *  @author wm
     *  @createTime 2023/9/17 12:47
     */
    private void changePlayMode() {
        playMode++;
        if (playMode > Constant.PLAY_MODE_SINGLE) {
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

}