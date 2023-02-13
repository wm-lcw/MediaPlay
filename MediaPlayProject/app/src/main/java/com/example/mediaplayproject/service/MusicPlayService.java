package com.example.mediaplayproject.service;

import android.app.Notification;
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

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.MusicPlayActivity;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicPlayerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private boolean firstPlay = true, isInitPlayHelper = false;
    private RemoteViews remoteViews;
    private static NotificationManager notificationManager;
    private Notification notification;
    private static final int NOTIFICATION_ID = 1;
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String PREV = "prev";
    public static final String NEXT = "next";
    public static final String CLOSE = "close";
    public static final String DELETE_MUSIC_ACTION = "com.example.media.play.delete.music.action";
    /**
     * playMode:����ģʽ 0->ѭ������; 1->�������; 2->��������;
     * ��Ҫ�ǿ��Ʋ�����������position
     */
    private int playMode = 0;

    /**
     * musicListMode:���ŵ���Դ 0->Ĭ���б�; 1->�ղ��б�; ���������չ�������б�
     */
    private int musicListMode = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        //����֪ͨ��ͨ��
        createNotificationChannel();
        //ע��㲥������
        registerMusicReceiver();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        helper.destroy();
        if (musicReceiver != null) {
            //�����̬ע��Ĺ㲥
            unregisterReceiver(musicReceiver);
        }
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title initPlayData
     * @author wm
     * @createTime 2023/2/11 15:44
     * @description ��Activity�е�һЩ���Ժ�״̬ͬ����Service��
     */
    public void initPlayData(List<MediaFileBean> musicInfo, int position, int musicListMode) {
        this.musicInfo = musicInfo;
        this.mPosition = position;
        this.musicListMode = musicListMode;
    }

    /**
     * @param seekBar          �������Ž�����
     * @param currentMusicInfo ��ǰ������Ϣ
     * @param currentTime      ��ǰ�������ŵ�ʱ��
     * @param mediaTime        ��ǰ��������ʱ��
     * @param handler          ���ڸ�Activity������Ϣ��Handler
     * @version V1.0
     * @Title initPlayHelper
     * @author wm
     * @createTime 2023/2/8 15:58
     * @description ��ʼ�����ֲ�����������
     */
    public void initPlayHelper(SeekBar seekBar, TextView currentMusicInfo, TextView currentTime, TextView mediaTime, Handler handler) {
        //����handler����
        mHandler = handler;
        //seekBarΪ���ֲ��Ž�������tvCurrentMusicInfoΪ��ǰ���Ÿ�������Ϣ
        helper = MusicPlayerHelper.getInstance();
        helper.initData(seekBar, currentMusicInfo, currentTime, mediaTime);
        //ʵ�����ֲ�����ϵĻص�������������Ϻ���ݲ���ģʽ�Զ�������һ��
        helper.setOnCompletionListener(mp -> {
            playNextEnd();
        });
        isInitPlayHelper = true;
        //��ʼ��֮������ʾ֪ͨ��
        showNotify();
    }


    /**
     * @param mediaFileBean ��ǰ���ŵ����ֶ���
     * @param isRestPlayer  �Ƿ����¿�ʼ����
     * @param handler       handler�������ڸ�Activity������Ϣ
     * @param mPosition     ��ǰ���Ÿ������±�
     * @return
     * @version V1.0
     * @Title play
     * @author wm
     * @createTime 2023/2/8 16:02
     * @description ��������
     */
    public void play(MediaFileBean mediaFileBean, Boolean isRestPlayer, Handler handler, int mPosition) {
        if (!TextUtils.isEmpty(mediaFileBean.getData())) {
            this.mPosition = mPosition;
//            DebugLog.debug(String.format("��ǰ״̬��%s  �Ƿ��л�������%s", helper.isPlaying(), isRestPlayer));
            //��¼��ǰ�Ĳ���״̬,���ڸ�Activity����Message
            boolean isPlayingStatus = false;
            // ��ǰ���ǲ��ţ��������ͣ
            if (!isRestPlayer && helper.isPlaying()) {
                pause();
            } else {
                //�״β��Ÿ������л��������š���������
                helper.playByMediaFileBean(mediaFileBean, isRestPlayer);
                isPlayingStatus = true;
            }
            //����Meeage��MusicPlayActivity�����ڸ��²���ͼ��
            Message msg = new Message();
            msg.what = MusicPlayActivity.HANDLER_MESSAGE_REFRESH_PLAY_ICON;
            Bundle bundle = new Bundle();
            bundle.putBoolean("iconType", isPlayingStatus);
            msg.setData(bundle);
            handler.sendMessage(msg);
            updateNotificationShow(mPosition, isPlayingStatus);
            firstPlay = false;
        } else {
            DebugLog.debug("��ǰ���ŵ�ַ��Ч");
            Toast.makeText(mContext, "��ǰ���ŵ�ַ��Ч", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @version V1.0
     * @Title pause
     * @author wm
     * @createTime 2023/2/8 14:34
     * @description ��ͣ
     */
    public void pause() {
        helper.pause();
    }

    /**
     * @version V1.0
     * @Title playPre
     * @author wm
     * @createTime 2023/2/8 14:34
     * @description ������һ��
     */
    public void playPre() {
        //�������ź�ѭ�����ţ����ǰ��������б��˳�򲥷�
        if (playMode == 0 || playMode == 2) {
            //�����ǰ�ǵ�һ�ף��򲥷����һ��
            if (mPosition <= 0) {
                mPosition = musicInfo.size();
            }
            mPosition--;
        } else if (playMode == 1) {
            //�������
            mPosition = getRandomPosition();
        }
        play(musicInfo.get(mPosition), true, mHandler, mPosition);
    }

    /**
     * @version V1.0
     * @Title playNext
     * @author wm
     * @createTime 2023/2/8 16:11
     * @description ������һ��
     */
    public void playNext() {
        //�������ź�ѭ�����ţ����ǰ��������б��˳�򲥷�
        if (playMode == 0 || playMode == 2) {
            mPosition++;
            //�����һ�����ڸ���������ȡ��һ��
            if (mPosition >= musicInfo.size()) {
                mPosition = 0;
            }
        } else if (playMode == 1) {
            //�������
            mPosition = getRandomPosition();
        }
        play(musicInfo.get(mPosition), true, mHandler, mPosition);
    }

    /**
     * @version V1.0
     * @Title playNextEnd
     * @author wm
     * @createTime 2023/2/8 18:26
     * @description ������Ϻ��Զ�������һ�������ڻص�
     */
    private void playNextEnd() {
        //ѭ������
        if (playMode == 0) {
            mPosition++;
            //�����һ�����ڸ���������ȡ��һ��
            if (mPosition >= musicInfo.size()) {
                mPosition = 0;
            }
        } else if (playMode == 1) {
            //�������
            mPosition = getRandomPosition();
        }
        //�������ţ�mPositionû�иı䣬ֱ�����¿�ʼ����
        play(musicInfo.get(mPosition), true, mHandler, mPosition);
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title getRandomPosition
     * @author wm
     * @createTime 2023/2/8 18:11
     * @description �Ӹ����б��л�ȡ�������0~musicInfo.size()��
     */
    private int getRandomPosition() {
        Random random = new Random();
        int randomNum = Math.abs(random.nextInt() % musicInfo.size());
        DebugLog.debug("" + randomNum);
        return randomNum;
    }

    /**
     * @version V1.0
     * @Title isPlaying
     * @author wm
     * @createTime 2023/2/8 16:12
     * @description ���ص�ǰ�Ƿ����ڲ���
     */
    public boolean isPlaying() {
        return helper.isPlaying();
    }

    /**
     * @version V1.0
     * @Title getPosition
     * @author wm
     * @createTime 2023/2/8 16:12
     * @description �����÷����Ĺ���
     */
    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        this.mPosition = position;
    }

    /**
     * @version V1.0
     * @Title getInitResult
     * @author wm
     * @createTime 2023/2/8 16:12
     * @description �����÷����Ĺ���
     */
    public boolean getInitResult() {
        return isInitPlayHelper;
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title getFirstPlay
     * @author wm
     * @createTime 2023/2/8 16:12
     * @description �����÷����Ĺ���
     */
    public boolean getFirstPlay() {
        return firstPlay;
    }

    /**
     * @param
     * @return getPlayMode
     * @version V1.0
     * @Title getPlayMode
     * @author wm
     * @createTime 2023/2/8 17:30
     * @description ��ȡ����ģʽ
     */
    public int getPlayMode() {
        return playMode;
    }

    /**
     * @param mode ����ģʽ
     * @return
     * @version V1.0
     * @Title setPlayMode
     * @author wm
     * @createTime 2023/2/8 17:52
     * @description ���ò���ģʽ
     */
    public void setPlayMode(int mode) {
        playMode = mode;
    }

    public int getMusicListMode() {
        return musicListMode;
    }

    /**
     * @version V1.0
     * @Title createNotificationChannel
     * @author wm
     * @createTime 2023/2/8 14:33
     * @description ����֪ͨ��ͨ��
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "9527";
            CharSequence name = "PlayControl";
            String description = "֪ͨ��";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * @version V1.0
     * @Title showNotify
     * @author wm
     * @createTime 2023/2/8 14:32
     * @description ��ʾ֪ͨ��
     */
    private void showNotify() {
        remoteViews = getContentView();
        //����PendingIntent
        Intent it = new Intent(this, MusicPlayActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, it, 0);

        //����֪ͨ����Ϣ
        notification = new NotificationCompat.Builder(this, "9527")
                //����ͼ��
                .setSmallIcon(R.mipmap.ic_notify_icon)
                .setWhen(System.currentTimeMillis())
                //����
                //.setContentTitle("΢��")
                //������Ϣ
                //.setContentText("����һ������Ϣ")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //���õ��������Intent
                .setContentIntent(pi)
                //����layout
                .setCustomContentView(remoteViews)
                //�����֪ͨ��ȡ��֪ͨ
                //.setAutoCancel(true)
                .setTicker("���ڲ���")
                .setOngoing(true)
                //���ȼ�
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * @return RemoteViews
     * @version V1.0
     * @Title getContentView
     * @author wm
     * @createTime 2023/2/8 14:32
     * @description ��ȡ֪ͨ�����ֶ���
     */
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

        //֪ͨ���������رհ�ť�㲥����
        Intent intentClose = new Intent(CLOSE);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, intentClose, 0);
        //Ϊclose�ؼ�ע���¼�
        mRemoteViews.setOnClickPendingIntent(R.id.iv_notify_close, closePendingIntent);

        //�������б�Ϊ�գ���ʼ��֪ͨ���ĸ�����Ϣ
        if (musicInfo.size() > 0) {
            mRemoteViews.setTextViewText(R.id.tv_song_title, musicInfo.get(mPosition).getTitle());
            mRemoteViews.setTextViewText(R.id.tv_song_artist, musicInfo.get(mPosition).getArtist());
        }
        return mRemoteViews;
    }

    /**
     * @version V1.0
     * @Title registerMusicReceiver
     * @author wm
     * @createTime 2023/2/8 16:15
     * @description ע�ᶯ̬�㲥
     */
    private void registerMusicReceiver() {
        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY);
        intentFilter.addAction(PREV);
        intentFilter.addAction(NEXT);
        intentFilter.addAction(CLOSE);
        intentFilter.addAction(DELETE_MUSIC_ACTION);
        registerReceiver(musicReceiver, intentFilter);
    }

    /**
     * @author wm
     * @version V1.0
     * @Title
     * @createTime 2023/2/8 16:15
     * @description �㲥������ , ��������֪ͨ���Ĺ㲥
     * @return
     */
    public class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PLAY:
                    play(musicInfo.get(mPosition), firstPlay, mHandler, mPosition);
                    break;
                case PREV:
                    playPre();
                    //�����б���ʾ��ʱ������֪ͨ������������֮����Ҫˢ�²����б�
                    sendMessageRefreshPosition();
                    break;
                case NEXT:
                    playNext();
                    //�����б���ʾ��ʱ������֪ͨ������������֮����Ҫˢ�²����б�
                    sendMessageRefreshPosition();
                    break;
                case CLOSE:
                    closeApp();
                    break;
                case DELETE_MUSIC_ACTION:
                    //ɾ�������Ĺ㲥
                    int deletePosition = intent.getExtras().getInt("musicPosition");
                    disposeDeleteMusic(deletePosition);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @version V1.0
     * @Title closeApp
     * @author wm
     * @createTime 2023/2/13 15:07
     * @description ���֪ͨ����ť�ر�����Ӧ��
     */
    private void closeApp() {
        BasicApplication.getActivityManager().finishAll();
    }

    /**
     * @version V1.0
     * @Title disposeDeleteMusic
     * @author wm
     * @createTime 2023/2/13 15:08
     * @description ɾ�����ֵ��жϺʹ���
     */
    private void disposeDeleteMusic(int deletePosition) {
        //�����õ���musicListSize��ɾ�����ֵ��mPosition��ɾ����λ��
        if (musicInfo.size() <= 0) {
            //����б�Ϊ�գ�֤��ɾ���������һ�׸裬�б�Ϊ�գ���Ҫֹͣ����
            toStop();
        } else {
            //��ɾ����������λ�õĸ�������Ӱ�쵱ǰ���ţ�ֻ��Ҫ��Activity�ϸ�UI���ٸ���list��position����
            if (deletePosition == mPosition) {
                //���б�Ϊ�գ���ɾ�����ǵ�ǰ���Ÿ���������Ҫ�������߼��ϵĴ���
                //ɾ���������б��������ƣ�positionָ��������׸��ˣ�������Ҫ��һ�ٲ�����һ����ֱ�Ӳ��ſ��ܻ����Խ�����⣩
                mPosition--;
                playNext();
                //������Ϣ��Activity����position
                sendMessageRefreshPosition();
            }
        }
    }

    /**
     * @version V1.0
     * @Title sendMessageRefreshPosition
     * @author wm
     * @createTime 2023/2/13 15:16
     * @description ������Ϣ��Activity����position
     */
    private void sendMessageRefreshPosition() {
        //����Message��MusicPlayActivity������ɾ�������position
        Message msg = new Message();
        msg.what = MusicPlayActivity.HANDLER_MESSAGE_REFRESH_POSITION;
        Bundle bundle = new Bundle();
        bundle.putInt("newPosition", mPosition);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * @version V1.0
     * @Title toStop
     * @author wm
     * @createTime 2023/2/12 22:08
     * @description ����ǰ���ŵ����ղ��б���ɾ�������и�������ֹͣ����
     */
    private void toStop() {
        helper.stop();
        //���ղ��б�Ϊ��֮�󣬲��ŵ��б�תΪĬ���б���Ҫˢ��Activity��֪ͨ��������
        //��֪ͨActivity�޸Ĳ����б�mPosition���ٵ���Service�е�initPlayHelper��������ʾ֪ͨ��
        firstPlay = true;

        //����Message��MusicPlayActivity��ɾ���ղ��б�֮���Զ��л���Ĭ���б�
        Message msg = new Message();
        msg.what = MusicPlayActivity.HANDLER_MESSAGE_TURN_TO_DEFAULT_LIST;
        mHandler.sendMessage(msg);
    }

    /**
     * @param position     ����λ��, changeToPlay ����λ��
     * @param changeToPlay true��ʾ��������״̬�ǲ��ţ�false��������״̬����ͣ
     * @return
     * @version V1.0
     * @Title updateNotificationShow
     * @author wm
     * @createTime 2023/2/8 16:16
     * @description ����֪ͨ����Ϣ��UI
     */
    public void updateNotificationShow(int position, boolean changeToPlay) {
        if (changeToPlay) {
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_pause_style);
        } else {
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_play_style);
        }
        //����ר��
//        remoteViews.setImageViewBitmap(R.id.iv_album_cover, MusicUtils.getAlbumPicture(this, mList.get(position).getPath(), 0));
        //������
        remoteViews.setTextViewText(R.id.tv_song_title, musicInfo.get(position).getTitle());
        //������
        remoteViews.setTextViewText(R.id.tv_song_artist, musicInfo.get(position).getArtist());

        //����֪ͨ
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

}