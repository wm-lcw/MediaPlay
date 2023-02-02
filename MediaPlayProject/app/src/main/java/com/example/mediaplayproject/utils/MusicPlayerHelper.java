package com.example.mediaplayproject.utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.mediaplayproject.bean.MediaFileBean;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * @author wm
 * @Classname MusicPlayerHelper
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/2/1 19:10
 * @Created by wm
 */
public class MusicPlayerHelper implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        SeekBar.OnSeekBarChangeListener {
    private static int MSG_CODE = 0x01;
    private static long MSG_TIME = 1_000L;

    private MusicPlayerHelperHanlder mHandler;
    /**
     * 播放器
     */
    private MediaPlayer player;

    /**
     * 进度条
     */
    private SeekBar seekBar;

    /**
     * 显示播放信息
     */
    private TextView text;

    /**
     * 当前的播放歌曲信息
     */
    private MediaFileBean mediaFileBean;

    public MusicPlayerHelper(SeekBar seekBar, TextView text) {
        mHandler = new MusicPlayerHelperHanlder(this);
        player = new MediaPlayer();
        // 设置媒体流类型
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnBufferingUpdateListener(this);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);

        this.seekBar = seekBar;
        this.seekBar.setOnSeekBarChangeListener(this);
        this.text = text;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBar.setSecondaryProgress(percent);
        int currentProgress =
                seekBar.getMax() * player.getCurrentPosition() / player.getDuration();
    }

    /**
     * 当前 Song 播放完毕
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(mp);
        }
    }

    /**
     * 当前 Song 已经准备好
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }


    /**
     * 播放
     *
     * @param mediaFileBean    播放源
     * @param isRestPlayer true 切换歌曲 false 不切换
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
            // 准备自动播放 同步加载，阻塞 UI 线程
            // player.prepare()
            // 建议使用异步加载方式，不阻塞 UI 线程
            player.prepareAsync();
        } else {
            player.start();
        }
        //发送更新命令
        mHandler.sendEmptyMessage(MSG_CODE);
    }

    /**
     * 暂停
     */
    public void pause() {
        if (player.isPlaying()) {
            player.pause();
        }
        //移除更新命令
        mHandler.removeMessages(MSG_CODE);
    }

    /**
     * 停止
     */
    public void stop() {
        player.stop();
        seekBar.setProgress(0);
        text.setText("停止播放");
        //移除更新命令
        mHandler.removeMessages(MSG_CODE);
    }


    /**
     * 是否正在播放
     */
    public Boolean isPlaying() {
        return player.isPlaying();
    }

    /**
     * 消亡 必须在 Activity 或者 Frament onDestroy() 调用 以防止内存泄露
     */
    public void destroy() {
        // 释放掉播放器
        player.release();
        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 用于监听SeekBar进度值的改变
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    /**
     * 用于监听SeekBar开始拖动
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeMessages(MSG_CODE);
    }

    /**
     * 用于监听SeekBar停止拖动  SeekBar停止拖动后的事件
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        // 得到该首歌曲最长秒数
        int musicMax = player.getDuration();
        // SeekBar最大值
        int seekBarMax = seekBar.getMax();
        //计算相对当前播放器歌曲的应播放时间
        float msec = progress / (seekBarMax * 1.0F) * musicMax;
        // 跳到该曲该秒
        player.seekTo((int) msec);
        mHandler.sendEmptyMessageDelayed(MSG_CODE, MSG_TIME);
    }

    private String getCurrentPlayingInfo(int currentTime, int maxTime) {
        String info = String.format("正在播放:  %s\t\t", mediaFileBean.getTitle());
        return String.format("%s %s / %s", info, formatTime(currentTime), formatTime(maxTime));
    }

    /**
     * 定义一个方法用来格式化获取到的时间
     */
    public static String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            return (time / 1000 / 60) + ":0" + time / 1000 % 60;
        } else {
            return (time / 1000 / 60) + ":" + time / 1000 % 60;
        }
    }

    private OnCompletionListener mOnCompletionListener;


    /**
     * Register a callback to be invoked when the end of a media source
     * has been reached during playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnCompletionListener(@NonNull OnCompletionListener listener) {
        this.mOnCompletionListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when playback of
     * a media source has completed.
     */
    public interface OnCompletionListener {
        /**
         * Called when the end of a media source is reached during playback.
         *
         * @param mp the MediaPlayer that reached the end of the file
         */
        void onCompletion(MediaPlayer mp);
    }

    static class MusicPlayerHelperHanlder extends Handler {
        WeakReference<MusicPlayerHelper> weakReference;

        public MusicPlayerHelperHanlder(MusicPlayerHelper helper) {
            super(Looper.getMainLooper());
            this.weakReference = new WeakReference<>(helper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_CODE) {
                int pos = 0;
                //如果播放且进度条未被按压
                if (weakReference.get().player.isPlaying() && !weakReference.get().seekBar.isPressed()) {
                    int position = weakReference.get().player.getCurrentPosition();
                    int duration = weakReference.get().player.getDuration();
                    if (duration > 0) {
                        // 计算进度（获取进度条最大刻度*当前音乐播放位置 / 当前音乐时长）
                        pos = (int) (weakReference.get().seekBar.getMax() * position / (duration * 1.0f));
                    }
                    weakReference.get().text.setText(weakReference.get().getCurrentPlayingInfo(position, duration));
                }
                weakReference.get().seekBar.setProgress(pos);
                sendEmptyMessageDelayed(MSG_CODE, MSG_TIME);
            }
        }
    }
}
