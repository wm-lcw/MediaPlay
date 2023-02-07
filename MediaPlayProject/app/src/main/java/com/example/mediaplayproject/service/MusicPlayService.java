package com.example.mediaplayproject.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.MusicPlayActivity;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicPlayerHelper;

import java.util.ArrayList;
import java.util.List;

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
    private Handler mHandler;
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private int mPosition = 0;
    private IBinder myBinder = new MyBinder();
    private MusicReceiver musicReceiver;

    /**
     * ��������
     */
    public static final String PLAY = "play";
    /**
     * ������ͣ
     */
    public static final String PAUSE = "pause";
    /**
     * ��һ��
     */
    public static final String PREV = "prev";
    /**
     * ��һ��
     */
    public static final String NEXT = "next";
    /**
     * �ر�֪ͨ��
     */
    public static final String CLOSE = "close";
    /**
     * ���ȱ仯
     */
    public static final String PROGRESS = "progress";


    @Override
    public void onCreate() {
        DebugLog.debug("");
        super.onCreate();
        createNotificationChannel();
        registerMusicReceiver();
        showNotify();
    }

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
     * @param musicInfo
     * @param handler
     * @return
     * @version V1.0
     * @Title initPlayHelper
     * @author wm
     * @createTime 2023/2/4 14:50
     * @description ��ʼ�����ֲ�����������
     */
    public void initPlayHelper(SeekBar seekBar, TextView currentMusicInfo, TextView currentTime, TextView mediaTime, List<MediaFileBean> musicInfo, Handler handler) {
        //��������б�
        this.musicInfo = musicInfo;
        //����handler����
        mHandler = handler;
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
     * @param mPosition
     * @return
     * @version V1.0
     * @Title play
     * @author wm
     * @createTime 2023/2/4 14:54
     * @description ����
     */
    public void play(MediaFileBean mediaFileBean, Boolean isRestPlayer, Handler handler, int mPosition) {
        if (!TextUtils.isEmpty(mediaFileBean.getData())) {
            this.mPosition = mPosition;
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
        //�����ǰ�ǵ�һ�ף��򲥷����һ��
        if (mPosition <= 0) {
            mPosition = musicInfo.size();
        }
        mPosition--;
        play(musicInfo.get(mPosition), true, mHandler,mPosition);
    }

    public void playNext() {
        DebugLog.debug("playNext");
        mPosition++;
        //�����һ�����ڸ���������ȡ��һ��
        if (mPosition >= musicInfo.size()) {
            mPosition = 0;
        }
        play(musicInfo.get(mPosition), true, mHandler,mPosition);

    }

    public boolean isPlaying() {
        return helper.isPlaying();
    }

    @Override
    public void onDestroy() {
        DebugLog.debug("");
        super.onDestroy();
        helper.destroy();
        if (musicReceiver != null) {
            //�����̬ע��Ĺ㲥
            unregisterReceiver(musicReceiver);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void createNotificationChannel() {
        DebugLog.debug("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel_name";
            String description = "֪ͨ��";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotify(){
        DebugLog.debug("");
        //����PendingIntent
        Intent it = new Intent(this,MusicPlayActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, it, 0);

        //����֪ͨ����Ϣ
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1")
                //����ͼ��
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                //����
                //.setContentTitle("΢��")
                //������Ϣ
                //.setContentText("����һ������Ϣ")
                //���õ��������Intent
                .setContentIntent(pi)
                //����layout
                .setCustomContentView(getContentView())
                //�����֪ͨ��ȡ��֪ͨ
                //.setAutoCancel(true)
                .setTicker("���ڲ���")
                .setOngoing(true)
                //���ȼ�
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        //��ʾ֪ͨ
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

    private RemoteViews getContentView() {
        RemoteViews mRemoteViews = new RemoteViews(this.getPackageName(), R.layout.layout_notify_view);
        //֪ͨ����������һ�װ�ť�㲥����
        Intent intentPrev = new Intent(PREV);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, intentPrev, 0);
        //Ϊprev�ؼ�ע���¼�
        mRemoteViews.setOnClickPendingIntent(R.id.btn_play_prev, prevPendingIntent);

        //֪ͨ��������������ͣ��ť�㲥����  //���ڽ��չ㲥ʱ������ͼ��Ϣ
        Intent intentPlay = new Intent(PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, intentPlay, 0);
        //Ϊplay�ؼ�ע���¼�
        mRemoteViews.setOnClickPendingIntent(R.id.btn_play, playPendingIntent);

        //֪ͨ����������һ�װ�ť�㲥����
        Intent intentNext = new Intent(NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, intentNext, 0);
        //Ϊnext�ؼ�ע���¼�
        mRemoteViews.setOnClickPendingIntent(R.id.btn_play_next, nextPendingIntent);
//
//        //֪ͨ���������رհ�ť�㲥����
//        Intent intentClose = new Intent(CLOSE);
//        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, intentClose, 0);
//        //Ϊclose�ؼ�ע���¼�
//        mRemoteViews.setOnClickPendingIntent(R.id.btn_notification_close, closePendingIntent);

        return mRemoteViews;
    }

    /**
     * ע�ᶯ̬�㲥
     */
    private void registerMusicReceiver() {
        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY);
        intentFilter.addAction(PREV);
        intentFilter.addAction(NEXT);
        intentFilter.addAction(CLOSE);
        registerReceiver(musicReceiver, intentFilter);
    }


    /**
     * �㲥������ ���ڲ��ࣩ
     */
    public class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PLAY:
                    DebugLog.debug(PLAY+" or "+PAUSE);
                    break;
                case PREV:
                    DebugLog.debug(PREV);
                    break;
                case NEXT:
                    DebugLog.debug(NEXT);
                    break;
                case CLOSE:
                    DebugLog.debug(CLOSE);
                    break;
                default:
                    break;
            }
        }
    }



}