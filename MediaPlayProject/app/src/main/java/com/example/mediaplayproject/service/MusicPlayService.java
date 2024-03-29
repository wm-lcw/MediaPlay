package com.example.mediaplayproject.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
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
import com.example.mediaplayproject.utils.MediaPlayWidgetProvider;
import com.example.mediaplayproject.utils.MusicPlayerHelper;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wm
 * @Description: 音乐播放器service，用于支持后台播放
 */
public class MusicPlayService extends Service {

    private Context mContext;
    private MusicPlayerHelper helper;
    private Handler mHandler;
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private int mPosition = 0;
    private final IBinder myBinder = new MyBinder();
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
    private String musicListName = Constant.LIST_MODE_DEFAULT_NAME;

    private Long playTotalTime = 0L;

    private static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(
            2,
            5,
            2L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(3),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    @Override
    public void onCreate() {
        super.onCreate();
        //创建通知栏通道
        createNotificationChannel();
        //注册广播接收器
        registerMusicReceiver();
        // 开始计时器
        startRecordPlayTime();
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

    /**
     * @createTime 2023/2/11 15:44
     * @description 将Activity中的一些属性和状态同步到Service中
     */
    public void initPlayData(List<MediaFileBean> musicInfo, int position, String musicListName, int playMode) {
        this.musicInfo = musicInfo;
        this.mPosition = position;
        this.musicListName = musicListName;
        this.playMode = playMode;
    }

    /**
     * @param handler 用于给Activity发送消息的Handler
     * @createTime 2023/2/8 15:58
     * @description 初始化音乐播放器辅助类
     */
    public void initPlayHelper(Handler handler) {
        // 保存handler对象
        mHandler = handler;
        helper = MusicPlayerHelper.getInstance();
        helper.initData(handler);
        // 初始化时就将音乐信息传给helper，执行初始化操作
        if(musicInfo.size() > 0){
            helper.initMusic(musicInfo.get(mPosition));
        }

        // 实现音乐播放完毕的回调函数，播放完毕后根据播放模式自动播放下一首
        helper.setOnCompletionListener(mp -> playNextEnd());
        isInitPlayHelper = true;
        // 初始化之后再显示通知栏
        showNotify();

        // service起来的时候执行一次定时关闭的逻辑
        changeTimingOffTime();
    }

    /**
     *  计时器--计算播放音乐的总时长
     *  @author wm
     *  @createTime 2023/12/23 15:20
     */
    private void startRecordPlayTime() {
        playTotalTime = DataRefreshService.getTotalPlayTime();
        THREAD_POOL.execute(() -> {
            while (true) {
                if (helper != null && helper.isPlaying()){
                    playTotalTime++;
                    DataRefreshService.setTotalPlayTime(playTotalTime);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     *  控制播放暂停--提供给外部调用，外部不需要获取播放列表等信息，交由service来处理
     *  @author wm
     *  @createTime 2023/12/23 15:4
     */
    public void play(){
        play(musicInfo.get(mPosition),firstPlay, mPosition);
    }

    /**
     * @param mediaFileBean 当前播放的音乐对象
     * @param isRestPlayer  是否重新开始播放
     * @param mPosition     当前播放歌曲的下标
     * @createTime 2023/2/8 16:02
     * @description 播放音乐
     */
    public void play(MediaFileBean mediaFileBean, boolean isRestPlayer, int mPosition) {
        if (!TextUtils.isEmpty(mediaFileBean.getData())) {
            this.mPosition = mPosition;
            // 记录当前的播放状态, 用于更新Activity和通知栏的页面状态（避免此时player还没有来得及更新）
            boolean isPlayingStatus = false;
            // 当前若是播放，则进行暂停
            if (!isRestPlayer && helper.isPlaying()) {
                pause();
            } else {
                // 首次播放歌曲、切换歌曲播放、继续播放
                helper.playByMediaFileBean(mediaFileBean, isRestPlayer);
                isPlayingStatus = true;
                // 在播放时保存信息
                DataRefreshService.setLastPlayInfo(musicListName,mPosition,mediaFileBean.getId(),playMode);
            }
            firstPlay = false;

            // 更新各个Fragment的播放状态
            Message msg = new Message();
            msg.what = Constant.HANDLER_MESSAGE_REFRESH_PLAY_STATE;
            mHandler.sendMessageDelayed(msg, 300);
            updateNotificationShow(mPosition, isPlayingStatus);

            // 刷新桌面小部件
            Intent updateWidgetIntent = new Intent(mContext, MediaPlayWidgetProvider.class);
            updateWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            mContext.sendBroadcast(updateWidgetIntent);

//            // 刷新锁屏的UI
//            Intent updateLockIntent = new Intent();
//            updateLockIntent.setAction(Constant.REFRESH_PLAY_STATE_ACTION);
//            mContext.sendBroadcast(updateLockIntent);

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
        helper.removeHandleMessage();
        //单曲播放和循环播放，都是按照音乐列表的顺序播放
        if (playMode == Constant.PLAY_MODE_LOOP || playMode == Constant.PLAY_MODE_SINGLE) {
            // 如果当前是第一首，则播放最后一首
            if (mPosition <= 0) {
                mPosition = musicInfo.size();
            }
            mPosition--;
        } else if (playMode == Constant.PLAY_MODE_SHUFFLE) {
            // 随机播放
            mPosition = getRandomPosition();
        }
        dealToPlay();
    }

    /**
     * @createTime 2023/2/8 16:11
     * @description 播放下一首
     */
    public void playNext() {
        helper.removeHandleMessage();
        //单曲播放和循环播放，都是按照音乐列表的顺序播放
        if (playMode == Constant.PLAY_MODE_LOOP || playMode == Constant.PLAY_MODE_SINGLE) {
            mPosition++;
            // 如果下一曲大于歌曲数量则取第一首
            if (mPosition >= musicInfo.size()) {
                mPosition = 0;
            }
        } else if (playMode == Constant.PLAY_MODE_SHUFFLE) {
            // 随机播放
            mPosition = getRandomPosition();
        }
        dealToPlay();
    }

    /**
     * @createTime 2023/2/8 18:26
     * @description 播放完毕后自动播放下一曲，用于回调
     */
    private void playNextEnd() {
        helper.removeHandleMessage();
        //循环播放
        if (playMode == 0) {
            mPosition++;
            // 如果下一曲大于歌曲数量则取第一首
            if (mPosition >= musicInfo.size()) {
                mPosition = 0;
            }
        } else if (playMode == 1) {
            // 随机播放
            mPosition = getRandomPosition();
        }
        // 单曲播放，mPosition没有改变，直接重新开始播放
        dealToPlay();
    }

    /**
     *  判断是否是最近播放列表，若是最近播放列表需要发广播给Activity，由Activity执行播放，否则直接播放
     *  @author wm
     *  @createTime 2023/9/18 20:01
     */
    private void dealToPlay(){
        if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(musicListName)){
            // 若是最近播放列表的上下曲，需要特殊处理，由MainActivity统一处理
            Intent intent = new Intent(Constant.CHANGE_MUSIC_ACTION);
            Bundle bundle = new Bundle();
            bundle.putInt("position", mPosition);
            bundle.putString("musicListName", musicListName);
            intent.putExtras(bundle);
            mContext.sendBroadcast(intent);
        } else {
            play(musicInfo.get(mPosition), true, mPosition);
        }
    }

    /**
     * 从歌曲列表中获取随机数（0~musicInfo.size()）
     * @createTime 2023/2/8 18:11
     */
    private int getRandomPosition() {
        Random random = new Random();
        int randomNum = Math.abs(random.nextInt() % musicInfo.size());
        DebugLog.debug("" + randomNum);
        return randomNum;
    }

    /**
     *  返回当前播放状态
     *  @author wm
     *  @createTime 2023/2/8 16:12
     *  @return : boolean true：正在播放； false：暂停状态
     */
    public boolean isPlaying() {
        if (helper != null){
            return helper.isPlaying();
        } else {
            return false;
        }
    }

    /**
     *  获取当前播放歌曲的下标
     *  @author wm
     *  @createTime 2024/1/5 15:08
     * @return : int ： -1：列表为空； >=0 歌曲下标
     */
    public int getPosition() {
        if (musicInfo == null || musicInfo.size() <= 0){
            return -1;
        }
        return mPosition;
    }

    public void setPosition(int position) {
        this.mPosition = position;
    }

    /**
     *  返回service是否已成功初始化
     *  @author wm
     *  @createTime 2023/12/23 16:00
     *  @return : boolean true：已初始化； false：未初始化
     */
    public boolean getInitResult() {
        return isInitPlayHelper;
    }

    public boolean getFirstPlay() {
        return firstPlay;
    }

    /**
     *  获取播放模式
     *  @author wm
     *  @createTime 2024/1/5 15:18
     * @return : int 0->循环播放; 1->随机播放; 2->单曲播放;
     */
    public int getPlayMode() {
        return playMode;
    }

    public void setPlayMode(int mode) {
        playMode = mode;
    }

    public String getMusicListName() {
        return musicListName;
    }

    public boolean isFavorite(){
        return musicInfo.get(mPosition).isLike();
    }

    /**
     *  获取歌曲名
     *  @author wm
     *  @createTime 2024/1/5 15:18
     * @return : java.lang.String
     */
    public String getMusicTitle(){
        String title = "";
        if (musicInfo.size() > 0 && mPosition != -1){
            title = musicInfo.get(mPosition).getTitle();
        }
        return title;
    }

    /**
     *  获取歌手名
     *  @author wm
     *  @createTime 2024/1/5 15:19
     *  @return : java.lang.String
     */
    public String getMusicArtist(){
        String artist = "";
        if (musicInfo.size() > 0 && mPosition != -1){
            artist = musicInfo.get(mPosition).getArtist();
        }
        return artist;
    }

    /**
     *  获取歌曲的路径
     *  @author wm
     *  @createTime 2024/1/9 15:12
     *  @return : java.lang.String
     */
    public String getMusicPath(){
        String path = "";
        if (musicInfo.size() > 0 && mPosition != -1){
            path = musicInfo.get(mPosition).getData();
        }
        return path;
    }

    /**
     *  移除helper的handler信息，按压拖动条时暂停更新时间信息
     *  @author wm
     *  @createTime 2023/12/23 16:02
     */
    public void tempPauseSendMessage() {
        helper.tempPauseSendMessage();
    }

    /**
     *  更改播放进度的拖动条
     *  @author wm
     *  @createTime 2023/12/23 16:02
     * @param progress:
     * @param maxProgress:
     */
    public void changeSeekbarProgress(int progress, int maxProgress) {
        helper.changeSeekbarProgress(progress, maxProgress);
    }

    /**
     *  更改播放模式
     *  playMode:播放模式 0->循环播放; 1->随机播放; 2->单曲播放;
     *  @author wm
     *  @createTime 2023/12/21 11:31
     */
    public void changePlayMode() {
        playMode++;
        if (playMode > Constant.PLAY_MODE_SINGLE) {
            playMode = Constant.PLAY_MODE_LOOP;
        }
        // 保存上次播放的播放模式
        DataRefreshService.setLastPlayInfo(musicListName,mPosition,musicInfo.get(mPosition).getId(),playMode);
    }

    /**
     *  更改歌曲的收藏状态
     *  @author wm
     *  @createTime 2023/12/21 11:52
     */
    public void changFavoriteState(){
        boolean isLike = musicInfo.get(mPosition).isLike();
        musicInfo.get(mPosition).setLike(!isLike);
        if (!isLike){
            // 加入收藏
            DataRefreshService.addMusicToFavoriteList(musicInfo.get(mPosition));
        } else {
            // 取消收藏，需要传递当前的列表名去判断是否要刷新播放状态
            DataRefreshService.removeFavoriteMusic(musicListName, musicInfo.get(mPosition));
        }
        // 发送消息给Activity更新列表状态
        Message msg = new Message();
        msg.what = Constant.HANDLER_MESSAGE_REFRESH_LIST_STATE;
        mHandler.sendMessage(msg);
    }

    /**
     * @createTime 2023/2/8 14:33
     * @description 创建通知栏通道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "notificationServiceId";
            CharSequence name = "ControlNotification";
            String description = "通知栏";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * @createTime 2023/2/8 14:32
     * @description 显示通知栏
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private void showNotify() {
        remoteViews = getContentView();

        // 设置PendingIntent
        Intent it = new Intent(this, MainActivity.class);
        it.putExtra("notify", true);
        PendingIntent pi = PendingIntent.getActivity(this, 0, it, 0);

        // 创建通知栏信息
        notification = new NotificationCompat.Builder(this, "notificationServiceId")
                // 设置图标(这里缺少图标会报错)
                .setSmallIcon(R.mipmap.ic_notify_icon)
                .setWhen(System.currentTimeMillis())
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

        // 获取remoteViews之后，再初始化通知栏的歌曲信息
        updateNotificationShow(mPosition, isPlaying());
    }

    /**
     * @createTime 2023/2/8 14:32
     * @description 获取通知栏布局对象，设置布局的监听事件
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private RemoteViews getContentView() {
        RemoteViews mRemoteViews = new RemoteViews(this.getPackageName(), R.layout.layout_notify_view);
        //通知栏控制器上一首按钮广播操作
        Intent intentPrev = new Intent(PREV);
         PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, intentPrev, 0);
        //为prev控件注册事件
        mRemoteViews.setOnClickPendingIntent(R.id.btn_play_prev, prevPendingIntent);

        //通知栏控制器播放暂停按钮广播操作
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

        return mRemoteViews;
    }

    /**
     * 注册动态广播
     * @createTime 2023/2/8 16:15
     */
    private void registerMusicReceiver() {
        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY);
        intentFilter.addAction(PREV);
        intentFilter.addAction(NEXT);
        intentFilter.addAction(CLOSE);
        intentFilter.addAction(Constant.CHANGE_TIMING_OFF_TIME_ACTION);
        registerReceiver(musicReceiver, intentFilter);
    }

    /**
     * 广播接收器
     * @createTime 2023/2/8 16:15
     */
    public class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PLAY:
                    play();
                    break;
                case PREV:
                    playPre();
                    break;
                case NEXT:
                    playNext();
                    break;
                case CLOSE:
                    closeApp();
                    break;
                case Constant.CHANGE_TIMING_OFF_TIME_ACTION:
                    changeTimingOffTime();
                default:
                    break;
            }
        }
    }

    /**
     *  更改定时关闭应用的时间
     *  @author wm
     *  @createTime 2023/9/28 14:38
     */
    private void changeTimingOffTime() {
        DebugLog.debug("----");
        // 更新定时关闭时间之前，先将旧的handler消息清除
        mHandler.removeMessages(Constant.HANDLER_MESSAGE_DELAY_TIMING_OFF);
        int timingOffTime = (int) SharedPreferencesUtil.getData(Constant.TIMING_OFF_TIME,0);
        if (timingOffTime > 0){
            long delayTime = (long) timingOffTime * 60 * 1000;
            mHandler.sendEmptyMessageDelayed(Constant.HANDLER_MESSAGE_DELAY_TIMING_OFF, delayTime);
        }
    }

    /**
     * @createTime 2023/2/13 15:07
     * @description 点击通知栏按钮关闭整个应用
     */
    private void closeApp() {
        BasicApplication.getActivityManager().finishAll();
    }

    /**
     * 若当前播放的是收藏列表且删除了所有歌曲，则停止播放
     * @createTime 2023/2/12 22:08
     */
    public void toStop() {
        helper.stop();
        firstPlay = true;

        // 更新通知栏
        updateNotificationShow(-1, false);

        // 刷新桌面小部件
        Intent updateWidgetIntent = new Intent(mContext, MediaPlayWidgetProvider.class);
        updateWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        mContext.sendBroadcast(updateWidgetIntent);

//        // 刷新锁屏的UI
//        Intent updateLockIntent = new Intent();
//        updateLockIntent.setAction(Constant.REFRESH_PLAY_STATE_ACTION);
//        mContext.sendBroadcast(updateLockIntent);
    }

    /**
     * 更改通知栏的歌曲信息和UI可用状态
     * @param position     歌曲位置, changeToPlay 歌曲位置
     * @param changeToPlay 接下来的状态； true播放，false暂停
     */
    public void updateNotificationShow(int position, boolean changeToPlay) {
        DebugLog.debug(" position " + position);
        boolean listNotNull = position != -1;
        if (changeToPlay) {
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_pause_style);
        } else {
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_play_style);
        }
        if (listNotNull) {
            remoteViews.setTextViewText(R.id.tv_song_title, musicInfo.get(position).getTitle());
            remoteViews.setTextViewText(R.id.tv_song_artist, musicInfo.get(position).getArtist());
//            remoteViews.setImageViewResource(R.id.btn_play,
//                    isPlaying() ? R.drawable.set_notify_pause_style : R.drawable.set_notify_play_style);
            // 封面专辑，频繁地调用setImageViewBitmap更新图片，会导致通知栏挂掉（每设置一次都会将其存储起来）
            // 暂时的解决方案是：适当压缩图片尺寸，减小内存占用
            Bitmap bitmap = ToolsUtils.getAlbumPicture(this, musicInfo.get(position).getData(), true);
            if (bitmap != null){
                remoteViews.setImageViewBitmap(R.id.custom_song_icon, bitmap);
            } else {
                remoteViews.setImageViewResource(R.id.custom_song_icon, R.mipmap.ic_notify_icon);
            }
        } else {
            remoteViews.setTextViewText(R.id.tv_song_title, "");
            remoteViews.setTextViewText(R.id.tv_song_artist, "");
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_play_style);
        }

        // 设定通知栏各按钮的可用状态
        remoteViews.setBoolean(R.id.btn_play, "setEnabled", listNotNull);
        remoteViews.setBoolean(R.id.btn_play_prev, "setEnabled", listNotNull);
        remoteViews.setBoolean(R.id.btn_play_next, "setEnabled", listNotNull);

        // 发送通知
        notificationManager.notify(NOTIFICATION_ID, notification);
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
}