package com.example.mediaplayproject.utils;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.MainActivity;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.service.MusicPlayService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 * @Classname MediaPlayWidgetProvider
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/12/19 10:34
 * @Created by wm
 */
public class MediaPlayWidgetProvider extends AppWidgetProvider {

    public static final String PLAY = "media.play.project.play_widget";
    public static final String PAUSE = "media.play.project.pause_widget";
    public static final String PREV = "media.play.project.prev_widget";
    public static final String NEXT = "media.play.project.next_widget";

    private int mPosition = 0;
    private int playMode = Constant.PLAY_MODE_LOOP;
    private String musicListName = Constant.LIST_MODE_DEFAULT_NAME;
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private MusicPlayService musicService;


    public MediaPlayWidgetProvider() {
        super();
    }

    /**
     *  当该窗口小部件第一次添加到桌面时调用该方法，可添加多次但只第一次调用
     *  @author wm
     *  @createTime 2023/12/19 19:34
     * @param context: 上下文
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        DebugLog.debug("onEnabled-");
        if (musicService != null){
            initInfo();
        }
    }

    /**
     *  每次窗口小部件被添加或者被更新时会调用该方法
     *  @author wm
     *  @createTime 2023/12/19 18:33
     * @param context: 上下文
     * @param appWidgetManager: 小部件的管理类，可以调用updateAppWidget来更新小部件
     * @param appWidgetIds: 小部件的ID，每次添加时获取到的appWidgetIds数量都为1，证明只操作添加的那一个，旧的那些不做处理
     *                    这可能是该提供程序的所有AppWidget实例，也可能只是其中的一个子集
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        DebugLog.debug(" appWidgetIds.length " + appWidgetIds.length);
        for (int widgetId : appWidgetIds) {
            RemoteViews mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_view);

            // 点击图标进入App
//            Intent it = new Intent(context, MainActivity.class);
//            PendingIntent pi = PendingIntent.getActivity(context, 0, it, 0);
//            mRemoteViews.setOnClickPendingIntent(R.id.custom_song_icon, pi);

            // 通知栏控制器上一首按钮广播操作
            Intent intentPrev = new Intent(PREV);
            intentPrev.setClass(context, MediaPlayWidgetProvider.class);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(context, 0, intentPrev, PendingIntent.FLAG_CANCEL_CURRENT);
            //为prev控件注册事件
            mRemoteViews.setOnClickPendingIntent(R.id.btn_play_prev, prevPendingIntent);

            // 通知栏控制器播放暂停按钮广播操作
            Intent intentPlay = new Intent(PLAY);
            intentPlay.setClass(context, MediaPlayWidgetProvider.class);
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(context, 0, intentPlay, PendingIntent.FLAG_CANCEL_CURRENT);
            //为play控件注册事件
            mRemoteViews.setOnClickPendingIntent(R.id.btn_play, playPendingIntent);

            // 通知栏控制器下一首按钮广播操作
            Intent intentNext = new Intent(NEXT);
            intentNext.setClass(context, MediaPlayWidgetProvider.class);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 0, intentNext, PendingIntent.FLAG_CANCEL_CURRENT);
            // 为next控件注册事件
            mRemoteViews.setOnClickPendingIntent(R.id.btn_play_next, nextPendingIntent);

            if (musicService != null){
                // 新增小部件时，获取音乐信息
                updateWidgetShow(mRemoteViews, mPosition, musicService.isPlaying());
            }
            // 更新小部件
            appWidgetManager.updateAppWidget(widgetId, mRemoteViews);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        DebugLog.debug(" action " + intent.getAction());
        // 每次操作小部件之前都获取一下状态,如果App是关闭状态，就不执行操作
        musicService = BasicApplication.getMusicService();
        DebugLog.debug("service " + musicService);
        if (musicService == null){
            return;
        }
        initInfo();
        switch (intent.getAction()) {
            case PLAY:
                musicService.play(musicInfo.get(mPosition), musicService.getFirstPlay(), mPosition);
                break;
            case PREV:
                musicService.playPre();
                break;
            case NEXT:
                musicService.playNext();
                break;
            case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                // 更新所有小部件信息
                AppWidgetManager mWidgetManager = AppWidgetManager.getInstance(context);
                int[] widgetIds = mWidgetManager.getAppWidgetIds(new ComponentName(context, MediaPlayWidgetProvider.class));
                DebugLog.debug("---appWidgetIds " + widgetIds);
                if (widgetIds == null || widgetIds.length == 0){
                    return;
                }
                DebugLog.debug("---appWidgetIds " + widgetIds + "; " + widgetIds.length);
                for (int widgetId : widgetIds) {
                    RemoteViews mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_view);
                    if (musicService != null){
                        // 在这里更新显示音乐信息
                        updateWidgetShow(mRemoteViews, mPosition, musicService.isPlaying());
                    }
                    mWidgetManager.updateAppWidget(widgetId, mRemoteViews);
                }
                break;
            default:
                break;
        }
    }

    /**
     *  每删除一次窗口小部件就调用一次
     *  @author wm
     *  @createTime 2023/12/19 19:38
     * @param context: 
     * @param appWidgetIds: 
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        DebugLog.debug("");
    }

    /**
     *  当最后一个该窗口小部件删除时调用该方法
     *  @author wm
     *  @createTime 2023/12/19 19:38
     * @param context:
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        DebugLog.debug("");
    }


    /**
     *  初始化数据
     *  @author wm
     *  @createTime 2023/12/19 11:33
     */
    private void initInfo(){
        // 从DataRefreshService中获取音乐列表，上次播放的信息等
        playMode = DataRefreshService.getLastPlayMode();
        musicListName = DataRefreshService.getLastPlayListName();
        mPosition = DataRefreshService.getLastPosition();
        musicInfo = DataRefreshService.getMusicListByName(musicListName);
        musicService.initPlayData(musicInfo, mPosition, musicListName, playMode);
    }

    /**
     *  更新小部件的UI信息
     *  @author wm
     *  @createTime 2023/12/19 19:39
     * @param remoteViews: 需要更新的小部件
     * @param position: 音乐下标
     * @param changeToPlay: 是否在播放
     */
    public void updateWidgetShow(RemoteViews remoteViews, int position, boolean changeToPlay) {
        boolean enable = position == -1;
        setWidgetEnable(remoteViews, !enable);
        if (changeToPlay) {
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_pause_style);
        } else {
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_play_style);
        }
        //封面专辑
//        remoteViews.setImageViewBitmap(R.id.iv_album_cover, MusicUtils.getAlbumPicture(this, mList.get(position).getPath(), 0));
        if (position == -1) {
            remoteViews.setTextViewText(R.id.tv_song_title, "");
            remoteViews.setTextViewText(R.id.tv_song_artist, "");
        } else {
            remoteViews.setTextViewText(R.id.tv_song_title, musicInfo.get(position).getTitle());
            remoteViews.setTextViewText(R.id.tv_song_artist, musicInfo.get(position).getArtist());
        }

    }

    /**
     *  设置小部件的Ui是否可用
     *  @author wm
     *  @createTime 2023/12/19 19:39
     * @param rv: 
     * @param enable:
     */
    private void setWidgetEnable(RemoteViews rv, boolean enable){
        if (enable){
            rv.setTextViewText(R.id.tv_song_title, musicInfo.get(mPosition).getTitle());
            rv.setTextViewText(R.id.tv_song_artist, musicInfo.get(mPosition).getArtist());
            if (musicService.isPlaying()) {
                rv.setImageViewResource(R.id.btn_play, R.drawable.set_notify_pause_style);
            } else {
                rv.setImageViewResource(R.id.btn_play, R.drawable.set_notify_play_style);
            }
        } else {
            // 当前播放列表为空的情况
            rv.setTextViewText(R.id.tv_song_title,"");
            rv.setTextViewText(R.id.tv_song_artist, "");
            rv.setImageViewResource(R.id.btn_play, R.drawable.set_notify_play_style);
        }
        rv.setBoolean(R.id.btn_play,"setEnabled",enable);
        rv.setBoolean(R.id.btn_play_prev,"setEnabled",enable);
        rv.setBoolean(R.id.btn_play_next,"setEnabled",enable);
    }
}
