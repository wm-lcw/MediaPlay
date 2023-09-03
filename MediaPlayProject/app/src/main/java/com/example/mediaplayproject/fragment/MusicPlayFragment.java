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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.example.mediaplayproject.adapter.ListViewPagerAdapter;
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
    private boolean mRegistered = false, firstPlay = true, isPlaying = false;
    private AudioManager mAudioManager;
    private Handler mActivityHandle;
    private ArrayList<PlayListFragment> viewPagerLists;

    private MusicBroadcastReceiver mMusicBroadcastReceiver;
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private final String VOLUME_CHANGE_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private final String VOLUME_MUTE = "android.media.STREAM_MUTE_CHANGED_ACTION";

    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private List<MediaFileBean> defaultList = new ArrayList<>();
    private List<MediaFileBean> favoriteList = new ArrayList<>();
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

    public void setDataFromMainActivity(MusicPlayService service, Handler handler,ArrayList<PlayListFragment> viewPagerLists, String listName, int position) {
        DebugLog.debug("");
        this.mActivityHandle = handler;
        this.viewPagerLists = viewPagerLists;
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
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
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
            tvCurrentPlayTime.setText("00:00");
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
//                DebugLog.debug("current position " + mPosition);
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
                refreshListStatus();
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
                String listSource = intent.getExtras().getString("musicListSource");
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
    private void dealDeleteMusic(String listSource, int deletePosition) {
        // 删除的音乐列表是当前正在播放的列表,这里要判断的逻辑需要改动，情况比较复杂
        if (musicListName.equalsIgnoreCase(listSource)) {
            if (musicInfo.size() <= 0) {
                // 如果列表为空，证明删除的是最后一首歌，列表为空，需要停止播放
                stopPlayByDelete();
            } else {
                // 刷新列表Ui高亮状态
                int result = viewPagerLists.get(0).checkRefreshPosition(deletePosition);
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
                    viewPagerLists.get(0).setSelectPosition(mPosition);
                    viewPagerLists.get(0).setSelection(mPosition);
                }
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
        mPosition = 0;
        firstPlay = true;
        isPlaying = false;
        //UI上刷新播放信息和播放状态
        initPlayStateAndInfo();
        //刷新通知栏的播放按钮状态
        musicService.updateNotificationShow(-1, false);
        // 直接保存默认列表的第一首歌作为最后的播放，避免此时关闭应用，导致下次打开时无法获取上次播放的信息
        DataRefreshService.setLastPosition(mPosition);
        DataRefreshService.setLastMusicId(defaultList.get(mPosition).getId());
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

    public void refreshPlayState(boolean state, int newPosition, String musicListName, List<MediaFileBean> musicInfo) {
        DebugLog.debug("refresh play state ");
        // 更新播放状态和收藏状态
        isPlaying = state;
        mPosition = newPosition;
        this.musicListName = musicListName;
        this.musicInfo = musicInfo;
        initPlayStateAndInfo();
        refreshListStatus();
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
        DebugLog.debug("musicListName " + musicListName + "; position " + mPosition);
        for (PlayListFragment fragment : viewPagerLists) {
            if (musicListName.equalsIgnoreCase(fragment.getListName())) {
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