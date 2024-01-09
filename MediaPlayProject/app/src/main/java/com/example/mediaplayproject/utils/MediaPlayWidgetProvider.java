package com.example.mediaplayproject.utils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.service.MusicPlayService;

import java.util.Arrays;

/**
 * @author wm
 * @Classname MediaPlayWidgetProvider
 * @Date 2023/12/19 10:34
 */
public class MediaPlayWidgetProvider extends AppWidgetProvider {

    public static final String PLAY = "media.play.project.play_widget";
    public static final String PREV = "media.play.project.prev_widget";
    public static final String NEXT = "media.play.project.next_widget";

    private MusicPlayService musicService;

    public MediaPlayWidgetProvider() {
        super();
    }

    /**
     * 当该窗口小部件第一次添加到桌面时调用该方法，可添加多次但只第一次调用
     *
     * @param context: 上下文
     * @author wm
     * @createTime 2023/12/19 19:34
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        DebugLog.debug("onEnabled-");
    }

    /**
     * 每次窗口小部件被添加或者被更新时会调用该方法
     *
     * @param context:          上下文
     * @param appWidgetManager: 小部件的管理类，可以调用updateAppWidget来更新小部件
     * @param appWidgetIds:     小部件的ID，每次添加时获取到的appWidgetIds数量都为1，证明只操作添加的那一个，旧的那些不做处理
     *                          这可能是该提供程序的所有AppWidget实例，也可能只是其中的一个子集
     * @author wm
     * @createTime 2023/12/19 18:33
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

            if (musicService != null) {
                // 新增小部件时，获取音乐信息
                updateWidgetShow(mRemoteViews);
            }
            // 更新小部件
            appWidgetManager.updateAppWidget(widgetId, mRemoteViews);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        DebugLog.debug(" action " + intent.getAction());
        musicService = BasicApplication.getMusicService();
        if (musicService == null || !musicService.getInitResult()) {
            // 若service为空，或者当前PlayHelper还没初始化 就直接返回
            return;
        }
        switch (intent.getAction()) {
            case PLAY:
                musicService.play();
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
                if (widgetIds == null || widgetIds.length == 0) {
                    return;
                }
                DebugLog.debug("---appWidgetIds " + Arrays.toString(widgetIds) + "; " + widgetIds.length);
                for (int widgetId : widgetIds) {
                    RemoteViews mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_view);
                    if (musicService != null) {
                        // 更新显示音乐信息
                        updateWidgetShow(mRemoteViews);
                    }
                    mWidgetManager.updateAppWidget(widgetId, mRemoteViews);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 每删除一次窗口小部件就调用一次
     *
     * @param context:
     * @param appWidgetIds:
     * @author wm
     * @createTime 2023/12/19 19:38
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        DebugLog.debug("");
    }

    /**
     * 当最后一个该窗口小部件删除时调用该方法
     *
     * @param context:
     * @author wm
     * @createTime 2023/12/19 19:38
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        DebugLog.debug("");
    }

    /**
     * 更新小部件的UI信息
     *
     * @param remoteViews: 需要更新的小部件
     * @author wm
     * @createTime 2023/12/19 19:39
     */
    public void updateWidgetShow(RemoteViews remoteViews) {
        // 音乐下标: -1列表为空
        int mPosition = musicService.getPosition();
        boolean enable = mPosition > -1;
        boolean isPlaying = musicService.isPlaying();
        if (enable) {
            // 列表不为空
            remoteViews.setTextViewText(R.id.tv_song_title, musicService.getMusicTitle());
            remoteViews.setTextViewText(R.id.tv_song_artist, musicService.getMusicArtist());
            remoteViews.setImageViewResource(R.id.btn_play,
                    isPlaying ? R.drawable.set_notify_pause_style : R.drawable.set_notify_play_style);
            // 封面专辑
            String musicPath = musicService.getMusicPath();
            Bitmap bitmap = ToolsUtils.getAlbumPicture(musicService.getBaseContext(), musicPath, true);
            if (bitmap != null){
                remoteViews.setImageViewBitmap(R.id.custom_song_icon, bitmap);
            } else {
                remoteViews.setImageViewResource(R.id.custom_song_icon, R.mipmap.ic_notify_icon);
            }

        } else {
            // 当前播放列表为空的情况
            remoteViews.setTextViewText(R.id.tv_song_title, "");
            remoteViews.setTextViewText(R.id.tv_song_artist, "");
            remoteViews.setImageViewResource(R.id.btn_play, R.drawable.set_notify_play_style);
        }
        remoteViews.setBoolean(R.id.btn_play, "setEnabled", enable);
        remoteViews.setBoolean(R.id.btn_play_prev, "setEnabled", enable);
        remoteViews.setBoolean(R.id.btn_play_next, "setEnabled", enable);



    }
}
