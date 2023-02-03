package com.example.mediaplayproject.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
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
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.MusicAdapter;
import com.example.mediaplayproject.bean.MediaFileBean;
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
public class MusicPlayActivity extends AppCompatActivity {

    private ImageView ivMediaLoop, ivMediaPre, ivMediaPlay, ivMediaNext, ivMediaList, ivCloseListView;
    private SeekBar sbVolume, sbProgress;
    private ListView mMusicListView;
    private TextView tvCurrentMusicInfo, tvCurrentPlayTime, tvMediaTime;
    private boolean  isShowList = false, mRegistered = false, firstPlay = true;
    private Context mContext;
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private LinearLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;
    private AudioManager mAudioManager;
    private MusicAdapter musicAdapter;
    private MusicPlayerHelper helper;
    private MusicBroadcastReceiver mMusicBroadcastReceiver;
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private final String VOLUME_CHANGE_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private final String VOLUME_MUTE = "android.media.STREAM_MUTE_CHANGED_ACTION";
    private final static int HANDLER_MESSAGE_REFRESH_VOLUME = 0;

    private int mPosition = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);
        mContext = this;
        updateMusicFiles();
        registerReceiver();
        initData();
        initPlayHelper();
    }


    /**
     * @param
     * @return
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
//                DebugLog.debug(mediaFileBean.getTitle() + " -- " + mediaFileBean.getData());
//            }
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //进入界面时刷新播放状态按钮
        ivMediaPlay.setImageResource(helper.isPlaying() ? R.mipmap.media_pause : R.mipmap.media_play);
        initVolume();
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title initVolume
     * @author wm
     * @createTime 2023/2/2 10:59
     * @description 初始化音量
     */
    private void initVolume() {
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        DebugLog.debug("getCurrentVolume " + currentVolume);
        if (currentVolume >= 0 && currentVolume <= 150) {
            int afterVolume = (int) (currentVolume / 1.5);
            sbVolume.setProgress(afterVolume);
        }
    }

    /**
     *  @version V1.0
     *  @Title initData
     *  @author wm
     *  @createTime 2023/2/3 17:58
     *  @description 获取组件、初始化数据
     *  @param
     *  @return
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

    }

    /**
     * @param
     * @version V1.0
     * @Title initPlayHelper
     * @author wm
     * @createTime 2023/2/2 11:00
     * @description 初始化音乐播放辅助类
     */
    private void initPlayHelper() {
        if (musicInfo.size() > 0) {
            tvCurrentMusicInfo.setText(musicInfo.get(mPosition).getTitle());
            tvCurrentPlayTime.setText("00:00");
            tvMediaTime.setText(MusicPlayerHelper.formatTime(musicInfo.get(mPosition).getDuration()));
            ivMediaPlay.setEnabled(true);
        } else {
            //若列表为空，则播放按钮不可点击
            tvCurrentMusicInfo.setText("");
            tvCurrentPlayTime.setText("");
            tvMediaTime.setText("");
            ivMediaPlay.setEnabled(false);
        }
        //sbProgress为音乐播放进度条，tvCurrentMusicInfo为当前播放歌曲的信息
        helper = new MusicPlayerHelper(sbProgress, tvCurrentMusicInfo, tvCurrentPlayTime, tvMediaTime);
        //实现音乐播放完毕的回调函数，播放完毕自动播放下一首（可以拓展为单曲播放、随机播放）
        helper.setOnCompletionListener(mp -> {
            DebugLog.debug("setOnCompletionListener ");
            playNext();
        });
    }

    /**
     *  @version V1.0
     *  @Title playPre
     *  @author wm
     *  @createTime 2023/2/3 18:11
     *  @description 播放上一首
     *  @param
     *  @return
     */
    private void playPre() {
        DebugLog.debug("playPre -- position  " + mPosition);
        //如果当前是第一首，则播放最后一首
        if (mPosition <= 0) {
            mPosition = musicInfo.size();
        }
        mPosition--;
        play(musicInfo.get(mPosition), true);
    }

    /**
     *  @version V1.0
     *  @Title playNext
     *  @author wm
     *  @createTime 2023/2/3 18:11
     *  @description 播放下一首
     *  @param
     *  @return
     */
    private void playNext() {
        DebugLog.debug("playNext -- position  " + mPosition);
        mPosition++;
        //如果下一曲大于歌曲数量则取第一首
        if (mPosition >= musicInfo.size()) {
            mPosition = 0;
        }
        play(musicInfo.get(mPosition), true);
    }

    /**
     *  @version V1.0
     *  @Title play
     *  @author wm
     *  @createTime 2023/2/3 18:11
     *  @description 播放音乐：mediaFileBean为歌曲文件对象，isRestPlayer是否从头开始播放
     *  @param mediaFileBean,isRestPlayer
     *  @return
     */
    private void play(MediaFileBean mediaFileBean, Boolean isRestPlayer) {
        if (!TextUtils.isEmpty(mediaFileBean.getData())) {
            DebugLog.debug(String.format("当前状态：%s  是否切换歌曲：%s", helper.isPlaying(), isRestPlayer));
            // 当前若是播放，则进行暂停
            if (!isRestPlayer && helper.isPlaying()) {
                pause();
                ivMediaPlay.setImageResource(R.mipmap.media_play);
            } else {
                //首次播放歌曲/切换歌曲播放
                helper.playByMediaFileBean(mediaFileBean, isRestPlayer);
                ivMediaPlay.setImageResource(R.mipmap.media_pause);
                // 正在播放的列表进行更新哪一首歌曲正在播放 主要是为了更新列表里面的显示
//                 for (int i = 0; i < musicInfo.size(); i++) {
//                     musicInfo.get(i).setPlaying(mPosition == i);
//                     musicAdapter.notifyItemChanged(i);
//                 }
            }
        } else {
            Toast.makeText(mContext, "当前播放地址无效", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  @version V1.0
     *  @Title pause
     *  @author wm
     *  @createTime 2023/2/3 18:17
     *  @description 暂停
     *  @param
     *  @return
     */
    private void pause() {
        helper.pause();
    }

    private View.OnClickListener mListener = new View.OnClickListener() {
        @SuppressLint("ResourceType")
        @Override
        public void onClick(View view) {
            if (view == ivMediaLoop) {
                DebugLog.debug("onclick ivMediaLoop");
            } else if (view == ivMediaPre) {
                DebugLog.debug("onclick ivMediaPre");
                playPre();
            } else if (view == ivMediaPlay) {
                DebugLog.debug("current position " + mPosition);
                // 判断当前是否是首次播放，若是首次播放，则需要设置重头开始播放（Media的首次播放需要reset等流程）
                play(musicInfo.get(mPosition), firstPlay);
                firstPlay = false;
            } else if (view == ivMediaNext) {
                DebugLog.debug("onclick ivMediaNext");
                playNext();
            } else if (view == ivMediaList) {
                DebugLog.debug("onclick ivMediaList");
                isShowList = true;
                createFloatView();
            }
        }
    };

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
     *  @version V1.0
     *  @Title createFloatView
     *  @author wm
     *  @createTime 2023/2/3 18:19
     *  @description 创建悬浮窗
     *  @param
     *  @return
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

//          设置悬浮窗口长宽数据
//        wmParams.width = 600;
//        wmParams.height = 1100;
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
                DebugLog.debug("close list");
                mWindowManager.removeView(mFloatLayout);
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
                play((MediaFileBean) musicInfo.get(position), true);
                //若从列表点击播放，则暂停播放按钮就设置为非首次播放
                firstPlay = false;
            }
        });

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
                DebugLog.debug("touch");
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
     * 注册音量广播接收器
     * @return
     */
    public void registerReceiver() {
        mMusicBroadcastReceiver = new MusicBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VOLUME_CHANGE_ACTION);
        filter.addAction(VOLUME_MUTE);
        mContext.registerReceiver(mMusicBroadcastReceiver, filter);
        mRegistered = true;
    }

    /**
     * 解注册音量广播监听器，需要与 registerReceiver 成对使用
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.destroy();
        unregisterReceiver();
    }

    /**
     *  @version V1.0
     *  @Title
     *  @author wm
     *  @createTime 2023/2/3 18:21
     *  @description 创建广播接收器，接收音量(媒体音量)改变的广播
     *  @param
     *  @return
     */
    private class MusicBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DebugLog.debug("receiver action " + intent.getAction());
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
     *  @version V1.0
     *  @Title
     *  @author wm
     *  @createTime 2023/2/3 18:22
     *  @description 创建handler，用于更新音量拖动条
     *  @param 
     *  @return 
     */
    final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_REFRESH_VOLUME) {
                int volume = msg.getData().getInt("volume");
                if (volume >= 0 && volume <= 150) {
                    int afterVolume = (int) (volume / 1.5);
                    sbVolume.setProgress(afterVolume);
                }
            }
        }
    };


}