package com.example.mediaplayproject.base;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SearchFiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author wm
 * @Classname BasicApplication
 * @Description 工程管理
 * @Version 1.0.0
 * @Date 2023/2/9 10:59
 * @Created by wm
 */
public class BasicApplication extends Application {
    private static ActivityManager activityManager;
    private static BasicApplication application;
    private static Context context;
    /**
     * 默认的列表
     */
    private List<MediaFileBean> defaultList = new ArrayList<>();
    /**
     * 收藏的列表
     */
    private List<MediaFileBean> favoriteList = new ArrayList<>();
    /**
     * 用于帮助过滤收藏列表里的重复歌曲
     */
    private HashSet<MediaFileBean> musicListUtils = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        //声明Activity管理
        activityManager = new ActivityManager();
        context = getApplicationContext();
        application = this;
        searchMusic();
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title searchMusic
     * @author wm
     * @createTime 2023/2/11 15:46
     * @description 搜索设备里的音乐媒体文件
     */
    private void searchMusic() {
        SearchFiles mSearcherFiles = SearchFiles.getInstance(context);
        defaultList = mSearcherFiles.getMusicInfo();
        //打印输出音乐列表
//        if (musicInfo.size() > 0) {
//            Iterator<MediaFileBean> iterator = musicInfo.iterator();
//            while (iterator.hasNext()) {
//                MediaFileBean mediaFileBean = iterator.next();
//                DebugLog.debug(mediaFileBean.getTitle());
//            }
//        }
    }

    public void refreshDefaultList(List<MediaFileBean> newDefaultList) {
        defaultList.clear();
        defaultList = newDefaultList;
    }

    public List<MediaFileBean> getDefaultList() {
        return defaultList;
    }

    public List<MediaFileBean> getFavoriteList() {
        return favoriteList;
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title addMusicToFavoriteList
     * @author wm
     * @createTime 2023/2/11 15:46
     * @description 将歌曲加入收藏列表
     */
    public void addMusicToFavoriteList(MediaFileBean mediaFileBean) {
        DebugLog.debug("");
        //插入set集合中，过滤掉重复添加的歌曲
        if (musicListUtils.add(mediaFileBean)) {
            favoriteList.add(mediaFileBean);
        }
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title deleteMusicFromFavoriteList
     * @author wm
     * @createTime 2023/2/11 15:46
     * @description 从收藏列表移除歌曲
     */
    public void deleteMusicFromFavoriteList(MediaFileBean mediaFileBean) {
        DebugLog.debug("");
        if (musicListUtils.contains(mediaFileBean)) {
            musicListUtils.remove(mediaFileBean);
            //获取要删除歌曲在收藏列表的下标是多少，然后发送广播给Service处理（当前列表是否是收藏列表）
            int removePosition = favoriteList.indexOf(mediaFileBean);
            Intent intent = new Intent("com.example.media.play.delete.music.action");
            Bundle bundle = new Bundle();
            bundle.putInt("musicPosition", removePosition);
            intent.putExtras(bundle);
            context.sendBroadcast(intent);
            favoriteList.remove(mediaFileBean);
        }
        if (defaultList.contains(mediaFileBean)) {
            //需要删除默认列表中的收藏状态,直接操作对象
            mediaFileBean.setLike(false);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    public static ActivityManager getActivityManager() {
        return activityManager;
    }

    /**
     * 内容提供器
     *
     * @return
     */
    public static Context getContext() {
        return context;
    }

    public static BasicApplication getApplication() {
        return application;
    }
}

