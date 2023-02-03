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
 * @Description 音乐播放器辅助类
 * @Version 1.0.0
 * @Date 2023/2/1 19:10
 * @Created by wm
 */
public class MusicPlayerHelper implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        SeekBar.OnSeekBarChangeListener {
    private static int MSG_CODE = 0x01;
    private static long MSG_TIME = 1_000L;

    private MusicPlayerHelperHandler mHandler;
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

    /**
     *  @version V1.0
     *  @Title MusicPlayerHelper
     *  @author wm
     *  @createTime 2023/2/3 18:27
     *  @description 初始化播放器
     *  @param
     *  @return
     */
    public MusicPlayerHelper(SeekBar seekBar, TextView text) {
        mHandler = new MusicPlayerHelperHandler(this);
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

    /**
     *  @version V1.0
     *  @Title onBufferingUpdate
     *  @author wm
     *  @createTime 2023/2/3 18:30
     *  @description 缓存百分比
     *  @param
     *  @return
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBar.setSecondaryProgress(percent);
        int currentProgress =
                seekBar.getMax() * player.getCurrentPosition() / player.getDuration();
    }

    /**
     *  @version V1.0
     *  @Title onCompletion
     *  @author wm
     *  @createTime 2023/2/3 18:31
     *  @description 当前歌曲播放完毕会调用该方法
     *  @param
     *  @return
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mOnCompletionListener != null) {
            DebugLog.debug("onCompletion");
            mOnCompletionListener.onCompletion(mp);
        }
    }


    /**
     *  @version V1.0
     *  @Title onPrepared
     *  @author wm
     *  @createTime 2023/2/3 18:33
     *  @description 歌曲准备播放
     *  @param
     *  @return
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        DebugLog.debug("onPrepared");
        mp.start();
    }

    /**
     *  @version V1.0
     *  @Title playByMediaFileBean
     *  @author wm
     *  @createTime 2023/2/3 18:33
     *  @description 播放歌曲：mediaFileBean 播放源；isRestPlayer  true 切换歌曲 false 不切换
     *  @param mediaFileBean,isRestPlayer
     *  @return
     */
    public void playByMediaFileBean(@NonNull MediaFileBean mediaFileBean, @NonNull Boolean isRestPlayer) {
        this.mediaFileBean = mediaFileBean;
        DebugLog.debug("mediaFile " + mediaFileBean.getData());
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
//            try {
//                player.prepare();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            // 建议使用异步加载方式，不阻塞 UI 线程
            player.prepareAsync();
        } else {
            player.start();
        }
        //发送更新命令，用于更新播放器进度条
        mHandler.sendEmptyMessage(MSG_CODE);
    }

    /**
     *  @version V1.0
     *  @Title pause
     *  @author wm
     *  @createTime 2023/2/3 18:35
     *  @description 暂停
     *  @param
     *  @return
     */
    public void pause() {
        if (player.isPlaying()) {
            player.pause();
        }
        //移除更新命令
        mHandler.removeMessages(MSG_CODE);
    }

    /**
     *  @version V1.0
     *  @Title stop
     *  @author wm
     *  @createTime 2023/2/3 18:35
     *  @description 停止
     *  @param
     *  @return
     */
    public void stop() {
        player.stop();
        seekBar.setProgress(0);
        text.setText("");
        //移除更新命令
        mHandler.removeMessages(MSG_CODE);
    }

    /**
     *  @version V1.0
     *  @Title isPlaying
     *  @author wm
     *  @createTime 2023/2/3 18:35
     *  @description 是否正在播放
     *  @param
     *  @return
     */
    public Boolean isPlaying() {
        return player.isPlaying();
    }

    /**
     *  @version V1.0
     *  @Title destroy
     *  @author wm
     *  @createTime 2023/2/3 18:38
     *  @description 消亡 必须在 Activity或Fragment的onDestroy()调用 以防止内存泄露
     *  @param
     *  @return
     */
    public void destroy() {
        // 释放掉播放器
        player.release();
        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     *  @version V1.0
     *  @Title onProgressChanged
     *  @author wm
     *  @createTime 2023/2/3 18:41
     *  @description 用于监听SeekBar进度值的改变
     *  @param
     *  @return
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    /**
     *  @version V1.0
     *  @Title onStartTrackingTouch
     *  @author wm
     *  @createTime 2023/2/3 18:41
     *  @description 监听SeekBar开始拖动
     *  @param
     *  @return
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeMessages(MSG_CODE);
    }

    /**
     *  @version V1.0
     *  @Title onStopTrackingTouch
     *  @author wm
     *  @createTime 2023/2/3 18:41
     *  @description 监听SeekBar停止拖动 ,停止拖动后计算seekbar和歌曲的对应位置
     *  @param
     *  @return
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

    private OnCompletionListener mOnCompletionListener;

    /**
     *  @version V1.0
     *  @Title setOnCompletionListener
     *  @author wm
     *  @createTime 2023/2/3 18:46
     *  @description 注册在播放过程中到达媒体源末尾时要调用的回调
     *  @param
     *  @return
     */
    public void setOnCompletionListener(@NonNull OnCompletionListener listener) {
        this.mOnCompletionListener = listener;
    }

    /**
     *  @version V1.0
     *  @Title
     *  @author wm
     *  @createTime 2023/2/3 18:32
     *  @description 媒体源播放完成后要调用的回调的接口定义
     *  @param
     *  @return
     */
    public interface OnCompletionListener {
        /**
         * 在播放期间到达媒体源的末尾时调用
         */
        void onCompletion(MediaPlayer mp);
    }

    /**
     *  @version V1.0
     *  @Title
     *  @author wm
     *  @createTime 2023/2/3 18:49
     *  @description 创建handler，用于更新音乐播放器进度条
     *  @param
     *  @return
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

    /**
     *  @version V1.0
     *  @Title getCurrentPlayingInfo
     *  @author wm
     *  @createTime 2023/2/3 18:43
     *  @description 格式化当前正在播放歌曲的信息
     *  @param
     *  @return 
     */
    private String getCurrentPlayingInfo(int currentTime, int maxTime) {
        String info = String.format("正在播放:  %s\t\t", mediaFileBean.getTitle());
        return String.format("%s %s / %s", info, formatTime(currentTime), formatTime(maxTime));
    }

    /**
     *  @version V1.0
     *  @Title formatTime
     *  @author wm
     *  @createTime 2023/2/3 18:40
     *  @description 格式化获取到的时间
     *  @param
     *  @return
     */
    public static String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            return (time / 1000 / 60) + ":0" + time / 1000 % 60;
        } else {
            return (time / 1000 / 60) + ":" + time / 1000 % 60;
        }
    }
}
