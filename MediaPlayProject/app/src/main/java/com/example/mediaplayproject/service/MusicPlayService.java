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
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.MainActivity;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicPlayerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @ClassName: MusicPlayService
 * @Description: 音乐播放器service，用于支持后台播放
 * @Author: wm
 * @CreateDate: 2023/2/4
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
    /**
     * playMode:播放模式 0->循环播放; 1->随机播放; 2->单曲播放;
     * 主要是控制播放上下曲的position
     */
    private int playMode = 0;

    /**
     * musicListMode:播放的来源 0->默认列表; 1->收藏列表; 后面可以扩展其他的列表
     */
    private int musicListMode = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        //创建通知栏通道
        createNotificationChannel();
        //注册广播接收器
        registerMusicReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public void removeMessage() {
        helper.removeMessage();
    }

    public void changeSeekbarProgress(int progress, int maxProgress) {
        helper.changeSeekbarProgress(progress, maxProgress);
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
            //解除动态注册的广播
            unregisterReceiver(musicReceiver);
        }
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * @createTime 2023/2/11 15:44
     * @description 将Activity中的一些属性和状态同步到Service中
     */
    public void initPlayData(List<MediaFileBean> musicInfo, int position, int musicListMode, int playMode) {
        this.musicInfo = musicInfo;
        this.mPosition = position;
        this.musicListMode = musicListMode;
        this.playMode = playMode;
    }

    /**
     * @param handler 用于给Activity发送消息的Handler
     * @createTime 2023/2/8 15:58
     * @description 初始化音乐播放器辅助类
     */
    public void initPlayHelper(Handler handler) {
        //保存handler对象
        mHandler = handler;
        helper = MusicPlayerHelper.getInstance();
        helper.initData(handler);
        //实现音乐播放完毕的回调函数，播放完毕后根据播放模式自动播放下一首
        helper.setOnCompletionListener(mp -> {
            playNextEnd();
        });
        isInitPlayHelper = true;
        //初始化之后再显示通知栏
        showNotify();
    }

    /**
     * @param mediaFileBean 当前播放的音乐对象
     * @param isRestPlayer  是否重新开始播放
     * @param mPosition     当前播放歌曲的下标
     * @createTime 2023/2/8 16:02
     * @description 播放音乐
     */
    public void play(MediaFileBean mediaFileBean, Boolean isRestPlayer, int mPosition) {
        if (!TextUtils.isEmpty(mediaFileBean.getData())) {
            this.mPosition = mPosition;
            // 记录当前的播放状态,用于给Activity发送Message
            boolean isPlayingStatus = false;
            // 当前若是播放，则进行暂停
            if (!isRestPlayer && helper.isPlaying()) {
                pause();
            } else {
                //首次播放歌曲、切换歌曲播放、继续播放
                helper.playByMediaFileBean(mediaFileBean, isRestPlayer);
                isPlayingStatus = true;
                //播放的时候保存播放的歌曲Id
                DataRefreshService.setLastMusicId(mediaFileBean.getId());
            }
            // 发送Message给MusicPlayFragment，用于更新播放图标
            Message msg = new Message();
            msg.what = Constant.HANDLER_MESSAGE_REFRESH_PLAY_ICON;
            Bundle bundle = new Bundle();
            bundle.putInt("position",mPosition);
            bundle.putBoolean("iconType", isPlayingStatus);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            updateNotificationShow(mPosition, isPlayingStatus);
            firstPlay = false;
        } else {
            DebugLog.debug("当前播放地址无效");
            Toast.makeText(mContext, "当前播放地址无效", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @createTime 2023/2/8 14:34
     * @description 暂停
     */
    public void pause() {
        helper.pause();
    }

    /**
     * @createTime 2023/2/8 14:34
     * @description 播放上一首
     */
    public void playPre() {
        //单曲播放和循环播放，都是按照音乐列表的顺序播放
        if (playMode == 0 || playMode == 2) {
            //如果当前是第一首，则播放最后一首
            if (mPosition <= 0) {
                mPosition = musicInfo.size();
            }
            mPosition--;
        } else if (playMode == 1) {
            //随机播放
            mPosition = getRandomPosition();
        }
        play(musicInfo.get(mPosition), true, mPosition);
    }

    /**
     * @createTime 2023/2/8 16:11
     * @description 播放下一首
     */
    public void playNext() {
        //单曲播放和循环播放，都是按照音乐列表的顺序播放
        if (playMode == 0 || playMode == 2) {
            mPosition++;
            //如果下一曲大于歌曲数量则取第一首
            if (mPosition >= musicInfo.size()) {
                mPosition = 0;
            }
        } else if (playMode == 1) {
            //随机播放
            mPosition = getRandomPosition();
        }
        play(musicInfo.get(mPosition), true, mPosition);
    }

    /**
     * @createTime 2023/2/8 18:26
     * @description 播放完毕后自动播放下一曲，用于回调
     */
    private void playNextEnd() {
        //循环播放
        if (playMode == 0) {
            mPosition++;
            //如果下一曲大于歌曲数量则取第一首
            if (mPosition >= musicInfo.size()) {
                mPosition = 0;
            }
        } else if (playMode == 1) {
            //随机播放
            mPosition = getRandomPosition();
        }
        //单曲播放，mPosition没有改变，直接重新开始播放
        play(musicInfo.get(mPosition), true, mPosition);
    }

    /**
     * @createTime 2023/2/8 18:11
     * @description 从歌曲列表中获取随机数（0~musicInfo.size()）
     */
    private int getRandomPosition() {
        Random random = new Random();
        int randomNum = Math.abs(random.nextInt() % musicInfo.size());
        DebugLog.debug("" + randomNum);
        return randomNum;
    }

    /**
     * @createTime 2023/2/8 16:12
     * @description 返回当前是否正在播放
     */
    public boolean isPlaying() {
        return helper.isPlaying();
    }

    /**
     * @createTime 2023/2/8 16:12
     * @description 描述该方法的功能
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
     * @description 描述该方法的功能
     */
    public boolean getInitResult() {
        return isInitPlayHelper;
    }

    /**
     * @createTime 2023/2/8 16:12
     * @description 描述该方法的功能
     */
    public boolean getFirstPlay() {
        return firstPlay;
    }

    /**
     * @createTime 2023/2/8 17:30
     * @description 获取播放模式
     */
    public int getPlayMode() {
        return playMode;
    }

    /**
     * @param mode 播放模式
     * @createTime 2023/2/8 17:52
     * @description 设置播放模式
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
     * @description 创建通知栏通道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "9527";
            CharSequence name = "PlayControl";
            String description = "通知栏";
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
     * @description 显示通知栏
     */
    private void showNotify() {
        remoteViews = getContentView();
        //设置PendingIntent
        Intent it = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, it, 0);

        //创建通知栏信息
        notification = new NotificationCompat.Builder(this, "9527")
                //设置图标
                .setSmallIcon(R.mipmap.ic_notify_icon)
                .setWhen(System.currentTimeMillis())
                //标题
                //.setContentTitle("微信")
                //正文消息
                //.setContentText("你有一条新消息")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //设置点击启动的Intent
                .setContentIntent(pi)
                //设置layout
                .setCustomContentView(remoteViews)
                //点击后通知栏取消通知
                //.setAutoCancel(true)
                .setTicker("正在播放")
                .setOngoing(true)
                //优先级
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
     * @description 获取通知栏布局对象
     */
    private RemoteViews getContentView() {
        RemoteViews mRemoteViews = new RemoteViews(this.getPackageName(), R.layout.layout_notify_view);
        //通知栏控制器上一首按钮广播操作
        Intent intentPrev = new Intent(PREV);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, intentPrev, 0);
        //为prev控件注册事件
        mRemoteViews.setOnClickPendingIntent(R.id.btn_play_prev, prevPendingIntent);

        //通知栏控制器播放暂停按钮广播操作  //用于接收广播时过滤意图信息
        Intent intentPlay = new Intent(PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, intentPlay, 0);
        //为play控件注册事件
        mRemoteViews.setOnClickPendingIntent(R.id.btn_play, playPendingIntent);

        //通知栏控制器下一首按钮广播操作
        Intent intentNext = new Intent(NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, intentNext, 0);
        //为next控件注册事件
        mRemoteViews.setOnClickPendingIntent(R.id.btn_play_next, nextPendingIntent);

        //通知栏控制器关闭按钮广播操作
        Intent intentClose = new Intent(CLOSE);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, intentClose, 0);
        //为close控件注册事件
        mRemoteViews.setOnClickPendingIntent(R.id.iv_notify_close, closePendingIntent);

        //若音乐列表不为空，初始化通知栏的歌曲信息
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
     * @description 注册动态广播
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
     * @author wm
     * @version V1.0
     * @Title
     * @createTime 2023/2/8 16:15
     * @description 广播接收器 , 接收来自通知栏的广播
     * @return
     */
    public class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PLAY:
                    play(musicInfo.get(mPosition), firstPlay, mPosition);
                    break;
                case PREV:
                    playPre();
                    //播放列表显示的时候，下拉通知栏播放上下曲之后，需要刷新播放列表
                    sendMessageRefreshPosition();
                    break;
                case NEXT:
                    playNext();
                    //播放列表显示的时候，下拉通知栏播放上下曲之后，需要刷新播放列表
                    sendMessageRefreshPosition();
                    break;
                case CLOSE:
                    closeApp();
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
     * @description 点击通知栏按钮关闭整个应用
     */
    private void closeApp() {
        BasicApplication.getActivityManager().finishAll();
    }


    /**
     * @version V1.0
     * @Title sendMessageRefreshPosition
     * @author wm
     * @createTime 2023/2/13 15:16
     * @description 发送信息给Activity更新position
     */
    private void sendMessageRefreshPosition() {
        //发送Message给MusicPlayFragment，更新删除后的新position
        Message msg = new Message();
        msg.what = Constant.HANDLER_MESSAGE_REFRESH_POSITION;
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
     * @description 若当前播放的是收藏列表且删除了所有歌曲，则停止播放
     */
    public void toStop() {
        helper.stop();
        //若收藏列表为空之后，播放的列表转为默认列表；需要刷新Activity和通知栏的内容
        //先通知Activity修改播放列表及mPosition，再调用Service中的initPlayHelper来重新显示通知栏
        firstPlay = true;
    }

    /**
     * @param position     歌曲位置, changeToPlay 歌曲位置
     * @param changeToPlay true表示接下来的状态是播放，false接下来的状态是暂停
     * @return
     * @version V1.0
     * @Title updateNotificationShow
     * @author wm
     * @createTime 2023/2/8 16:16
     * @description 更改通知的信息和UI
     */
    public void updateNotificationShow(int position, boolean changeToPlay) {
        if (changeToPlay) {
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_pause_style);
        } else {
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_play_style);
        }
        //封面专辑
//        remoteViews.setImageViewBitmap(R.id.iv_album_cover, MusicUtils.getAlbumPicture(this, mList.get(position).getPath(), 0));
        //歌曲名
        remoteViews.setTextViewText(R.id.tv_song_title, musicInfo.get(position).getTitle());
        //歌手名
        remoteViews.setTextViewText(R.id.tv_song_artist, musicInfo.get(position).getArtist());

        //发送通知
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

}