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
 * @Description: ���ֲ�����service������֧�ֺ�̨����
 * @Author: wm
 * @CreateDate: 2023/2/4
 * @UpdateUser: updater
 * @UpdateDate: 2023/2/4
 * @UpdateRemark: ��������
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
     * @description ��ʼ�����ֲ�����������
     */
    public void initPlayHelper(SeekBar seekBar, TextView currentMusicInfo, TextView currentTime, TextView mediaTime) {
        //seekBarΪ���ֲ��Ž�������tvCurrentMusicInfoΪ��ǰ���Ÿ�������Ϣ
        helper = new MusicPlayerHelper(seekBar, currentMusicInfo, currentTime, mediaTime);
        //ʵ�����ֲ�����ϵĻص���������������Զ�������һ�ף�������չΪ�������š�������ţ�
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
     * @description ����
     */
    public void play(MediaFileBean mediaFileBean, Boolean isRestPlayer, Handler handler) {
        if (!TextUtils.isEmpty(mediaFileBean.getData())) {
            DebugLog.debug(String.format("��ǰ״̬��%s  �Ƿ��л�������%s", helper.isPlaying(), isRestPlayer));
            //��¼��ǰ�Ĳ���״̬
            boolean isPlayingStatus= false;
            // ��ǰ���ǲ��ţ��������ͣ
            if (!isRestPlayer && helper.isPlaying()) {
                pause();
            } else {
                //�״β��Ÿ������л���������
                helper.playByMediaFileBean(mediaFileBean, isRestPlayer);
                isPlayingStatus = true;
                // ���ڲ��ŵ��б���и�����һ�׸������ڲ��� ��Ҫ��Ϊ�˸����б��������ʾ
//                 for (int i = 0; i < musicInfo.size(); i++) {
//                     musicInfo.get(i).setPlaying(mPosition == i);
//                     musicAdapter.notifyItemChanged(i);
//                 }
            }
            //����Meeage��MusicPlayActivity�����ڸ��²���ͼ��
            Message msg = new Message();
            msg.what = MusicPlayActivity.HANDLER_MESSAGE_REFRESH_PLAY_ICON;
            Bundle bundle = new Bundle();
            bundle.putBoolean("iconType", isPlayingStatus);
            msg.setData(bundle);
            handler.sendMessage(msg);
        } else {
            DebugLog.debug("��ǰ���ŵ�ַ��Ч");
            Toast.makeText(mContext, "��ǰ���ŵ�ַ��Ч", Toast.LENGTH_SHORT).show();
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