package com.example.mediaplayproject.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaplayproject.activity.MusicPlayActivity;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicPlayerHelper;

/**
 * @ClassName: MusicPlayService
 * @Description: 音乐播放器service，用于支持后台播放
 * @Author: wm
 * @CreateDate: 2023/2/4
 * @UpdateUser: updater
 * @UpdateDate: 2023/2/4
 * @UpdateRemark: 更新内容
 * @Version: 1.0
 */
public class MusicPlayService extends Service {

    private Context mContext;
    private MusicPlayerHelper helper;
    private IBinder myBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public class MyBinder extends Binder {
        public MusicPlayService getService(Context context) {
            mContext = context;
            return MusicPlayService.this;
        }
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title initPlayHelper
     * @author wm
     * @createTime 2023/2/4 14:50
     * @description 初始化音乐播放器辅助类
     */
    public void initPlayHelper(SeekBar seekBar, TextView currentMusicInfo, TextView currentTime, TextView mediaTime) {
        //seekBar为音乐播放进度条，tvCurrentMusicInfo为当前播放歌曲的信息
        helper = new MusicPlayerHelper(seekBar, currentMusicInfo, currentTime, mediaTime);
        //实现音乐播放完毕的回调函数，播放完毕自动播放下一首（可以拓展为单曲播放、随机播放）
        helper.setOnCompletionListener(mp -> {
            DebugLog.debug("setOnCompletionListener ");
            playNext();
        });
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title play
     * @author wm
     * @createTime 2023/2/4 14:54
     * @description 播放
     */
    public void play(MediaFileBean mediaFileBean, Boolean isRestPlayer, Handler handler) {
        if (!TextUtils.isEmpty(mediaFileBean.getData())) {
            DebugLog.debug(String.format("当前状态：%s  是否切换歌曲：%s", helper.isPlaying(), isRestPlayer));
            //记录当前的播放状态
            boolean isPlayingStatus= false;
            // 当前若是播放，则进行暂停
            if (!isRestPlayer && helper.isPlaying()) {
                pause();
            } else {
                //首次播放歌曲、切换歌曲播放
                helper.playByMediaFileBean(mediaFileBean, isRestPlayer);
                isPlayingStatus = true;
                // 正在播放的列表进行更新哪一首歌曲正在播放 主要是为了更新列表里面的显示
//                 for (int i = 0; i < musicInfo.size(); i++) {
//                     musicInfo.get(i).setPlaying(mPosition == i);
//                     musicAdapter.notifyItemChanged(i);
//                 }
            }
            //发送Meeage给MusicPlayActivity，用于更新播放图标
            Message msg = new Message();
            msg.what = MusicPlayActivity.HANDLER_MESSAGE_REFRESH_PLAY_ICON;
            Bundle bundle = new Bundle();
            bundle.putBoolean("iconType", isPlayingStatus);
            msg.setData(bundle);
            handler.sendMessage(msg);
        } else {
            DebugLog.debug("当前播放地址无效");
            Toast.makeText(mContext, "当前播放地址无效", Toast.LENGTH_SHORT).show();
        }
    }

    public void pause() {
        DebugLog.debug("pause");
        helper.pause();
    }

    public void playPre() {
        DebugLog.debug("playPre");
    }

    public void playNext() {
        DebugLog.debug("playNext");
    }

    public boolean isPlaying() {
        return helper.isPlaying();
    }

    public void destroy() {
        DebugLog.debug("service destroy");
        helper.destroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}