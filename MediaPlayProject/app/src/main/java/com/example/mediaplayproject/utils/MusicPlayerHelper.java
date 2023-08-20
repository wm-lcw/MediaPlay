package com.example.mediaplayproject.utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.mediaplayproject.activity.MusicPlayActivity;
import com.example.mediaplayproject.bean.MediaFileBean;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * @author wm
 * @Classname MusicPlayerHelper
 * @Description 音乐播放器辅助类
 * @Version 1.0.0
 * @Date 2023/2/1 19:10
 * @Created by wm
 */
public class MusicPlayerHelper implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private static int MSG_CODE = 0x01;
    private static long MSG_TIME = 1_000L;


    private MusicPlayerHelperHandler mHandler;
    /**
     * 播放器
     */
    private MediaPlayer player;

    /**
     * 当前的播放歌曲信息
     */
    private MediaFileBean mediaFileBean;

    private Handler mActivityHandle;
    private int currentProgress;
    private int maxProgress;
    private String currentMusicInfo, currentPlayTime, mediaTime;
    private boolean isPressed = false;

    private static MusicPlayerHelper instance = new MusicPlayerHelper();

    private MusicPlayerHelper() {
        mHandler = new MusicPlayerHelperHandler(this);
        player = new MediaPlayer();
        // 设置媒体流类型
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnBufferingUpdateListener(this);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
    }

    public static MusicPlayerHelper getInstance() {
        return instance;
    }

    public void initData(Handler handler) {
        this.mActivityHandle = handler;
    }

    /**
     * @createTime 2023/2/3 18:30
     * @description 缓存百分比
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    /**
     * @createTime 2023/2/3 18:31
     * @description 当前歌曲播放完毕会调用该方法
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(mp);
        }
    }


    /**
     * @createTime 2023/2/3 18:33
     * @description 歌曲准备播放
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    /**
     * @param mediaFileBean 播放源
     * @param isRestPlayer  true 切换歌曲 false 不切换
     * @createTime 2023/2/3 18:33
     * @description 播放歌曲
     */
    public void playByMediaFileBean(@NonNull MediaFileBean mediaFileBean, @NonNull Boolean isRestPlayer) {
        this.mediaFileBean = mediaFileBean;
        if (isRestPlayer) {
            //重置多媒体
            player.reset();
            // 设置数据源
            if (!TextUtils.isEmpty(mediaFileBean.getData())) {
                try {
                    player.setDataSource(mediaFileBean.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 使用异步加载方式，不阻塞 UI 线程
            player.prepareAsync();
        } else {
            player.start();
        }
        //发送更新命令，用于更新播放器进度条
        mHandler.sendEmptyMessage(MSG_CODE);
    }

    /**
     * @createTime 2023/2/3 18:35
     * @description 暂停
     */
    public void pause() {
        if (player.isPlaying()) {
            player.pause();
        }
        //移除更新命令
        mHandler.removeMessages(MSG_CODE);
    }

    /**
     * @createTime 2023/2/3 18:35
     * @description 停止
     */
    public void stop() {
        //音乐正在播放时才调用stop，未初始化的状态调用stop会报-38的错误；
        if (player != null && player.isPlaying()) {
            player.stop();
            player.reset();
        }
        currentProgress = 0;
        currentPlayTime = "00:00";

        //移除更新命令
        mHandler.removeMessages(MSG_CODE);
    }

    /**
     * @author wm
     * @createTime 2023/2/3 18:35
     * @description 是否正在播放
     */
    public Boolean isPlaying() {
        return player.isPlaying();
    }

    /**
     * @author wm
     * @createTime 2023/2/3 18:38
     * @description 消亡 必须在 Activity或Fragment的onDestroy()调用 以防止内存泄露
     */
    public void destroy() {
        // 释放掉播放器
        player.release();
        mHandler.removeCallbacksAndMessages(null);
    }


    private OnCompletionListener mOnCompletionListener;

    /**
     * @createTime 2023/2/3 18:46
     * @description 注册在播放过程中到达媒体源末尾时要调用的回调
     */
    public void setOnCompletionListener(@NonNull OnCompletionListener listener) {
        this.mOnCompletionListener = listener;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        DebugLog.debug("");
        return true;
    }

    /**
     * 拖动条正在按压，暂停更新拖动条和播放时间
     *
     * @author wm
     * @createTime 2023/8/20 21:49
     */
    public void removeMessage() {
        isPressed = true;
        mHandler.removeMessages(MSG_CODE);
    }

    /**
     * 根据拖动条变化，跳转歌曲的位置
     *
     * @param progress:    拖动条位置
     * @param maxProgress: 拖动条最大数值，用来辅助计算播放器要跳转的秒数
     * @author wm
     * @createTime 2023/8/20 21:47
     */
    public void changeSeekbarProgress(int progress, int maxProgress) {
        isPressed = false;
        this.maxProgress = maxProgress;
        // 得到该首歌曲最长秒数
        int musicMax = player.getDuration();
        //计算相对当前播放器歌曲的应播放时间
        float second = progress / (maxProgress * 1.0F) * musicMax;
        // 跳到该曲该秒
        player.seekTo((int) second);
        mHandler.sendEmptyMessageDelayed(MSG_CODE, MSG_TIME);
    }

    /**
     * @createTime 2023/2/3 18:32
     * @description 媒体源播放完成后要调用的回调的接口定义
     */
    public interface OnCompletionListener {
        /**
         * 在播放期间到达媒体源的末尾时调用
         */
        void onCompletion(MediaPlayer mp);
    }

    /**
     * @createTime 2023/2/3 18:49
     * @description 创建handler，用于更新音乐播放器进度条
     */
    static class MusicPlayerHelperHandler extends Handler {
        WeakReference<MusicPlayerHelper> weakReference;

        public MusicPlayerHelperHandler(MusicPlayerHelper helper) {
            super(Looper.getMainLooper());
            this.weakReference = new WeakReference<>(helper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_CODE) {
                int seekbarProgress = 0;
                String currentPlayingInfo = weakReference.get().getCurrentPlayingInfo();
                String currentTime = weakReference.get().currentPlayTime;
                String mediaTime = weakReference.get().mediaTime;
                //如果播放且进度条未被按压
                if (weakReference.get().player.isPlaying() && !weakReference.get().isPressed) {
                    int position = weakReference.get().player.getCurrentPosition();
                    int duration = weakReference.get().player.getDuration();
                    if (duration > 0) {
                        // 计算进度（获取进度条最大刻度*当前音乐播放位置 / 当前音乐时长）
                        seekbarProgress = (int) (weakReference.get().maxProgress * position / (duration * 1.0f));
                    }
                    currentTime = weakReference.get().getFormatTime(position);
                    mediaTime = weakReference.get().getFormatTime(duration);
                }
                Message message = new Message();
                message.what = MusicPlayActivity.HANDLER_MESSAGE_REFRESH_FROM_PLAY_HELPER;
                Bundle bundle = new Bundle();
                bundle.putInt("seekbarProgress", seekbarProgress);
                bundle.putString("currentPlayingInfo", currentPlayingInfo);
                bundle.putString("currentTime", currentTime);
                bundle.putString("mediaTime", mediaTime);
                message.setData(bundle);
                weakReference.get().getMActivityHandle().sendMessage(message);
                sendEmptyMessageDelayed(MSG_CODE, MSG_TIME);
            }
        }
    }

    /**
     * @createTime 2023/2/3 18:43
     * @description 格式化当前正在播放歌曲的信息
     */
    private String getCurrentPlayingInfo() {
        return String.format("%s", mediaFileBean.getTitle());
    }

    private String getFormatTime(int time) {
        return String.format("%s", formatTime(time));
    }

    private Handler getMActivityHandle() {
        return mActivityHandle;
    }

    /**
     * @author wm
     * @createTime 2023/2/3 18:40
     * @description 格式化获取到的时间
     */
    public static String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            return (time / 1000 / 60) + ":0" + time / 1000 % 60;
        } else {
            return (time / 1000 / 60) + ":" + time / 1000 % 60;
        }
    }
}
