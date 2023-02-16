package com.example.mediaplayproject.base;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicDataBaseHelper;
import com.example.mediaplayproject.utils.SearchFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private MusicDataBaseHelper musicDataBaseHelper;
    private SQLiteDatabase db;
    /**
     * 从数据库中获取的收藏列表标记
     */
    private HashMap<Long, Integer> musicListFromData = new HashMap<>();
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

    private int lastPlayListMode = 0, lastPlayMode = 0, lastPosition = 0;
    private long lastMusicId = 0;

    private String FAVORITE_LIST_TABLE_NAME = "musiclistrecord";
    private String LAST_MUSIC_INFO_TABLE_NAME = "lastplayinforecord";

    @Override
    public void onCreate() {
        super.onCreate();
        //声明Activity管理
        activityManager = new ActivityManager();
        context = getApplicationContext();
        application = this;
        musicDataBaseHelper = new MusicDataBaseHelper(context, "musicplay.db", null, 1);
        db = musicDataBaseHelper.getReadableDatabase();
        searchMusic();
        //初始化上次播放的相关信息
        initLastPlayInfo();
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
        //搜索设备里的音乐媒体文件
        defaultList = mSearcherFiles.getMusicInfo();
//        //打印输出音乐列表
//        if (defaultList.size() > 0) {
//            Iterator<MediaFileBean> iterator = defaultList.iterator();
//            while (iterator.hasNext()) {
//                MediaFileBean mediaFileBean = iterator.next();
//                DebugLog.debug(mediaFileBean.getTitle() + "---id " + mediaFileBean.getId());
//            }
//        }

        //从数据库中获取数据
        Cursor cursor = db.query(FAVORITE_LIST_TABLE_NAME, null, null, null, null, null, null);
        //存在数据才返回true
        if (cursor.moveToFirst()) {
            do {
                long musicId = cursor.getLong(cursor.getColumnIndex("musicId"));
                int isLike = cursor.getInt(cursor.getColumnIndex("isLike"));
                musicListFromData.put(musicId, isLike);
            } while (cursor.moveToNext());
        }
        cursor.close();

        //获取的列表与数据库中的数据对比，找出存在且收藏的歌，将其添加到收藏列表中
        favoriteList.clear();
        long id = 0;
        for (int i = 0; i < defaultList.size(); i++) {
            id = defaultList.get(i).getId();
            if (musicListFromData.containsKey(id)) {
                defaultList.get(i).setLike(1 == musicListFromData.get(id));
                //使用musicListUtils来筛选出重复的歌曲
                if (musicListUtils.add(defaultList.get(i))) {
                    favoriteList.add(defaultList.get(i));
                }
            }
        }

        DebugLog.debug("-- favoriteListSize " + favoriteList.size());

    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title initLastPlayInfo
     * @author wm
     * @createTime 2023/2/15 10:34
     * @description 初始化上次播放的信息
     */
    private void initLastPlayInfo() {
        //从数据库中获取数据
        Cursor cursor = db.query(LAST_MUSIC_INFO_TABLE_NAME, null, null, null, null, null, null);
        //存在数据才返回true
        if (cursor.moveToFirst()) {
            do {
                lastPlayListMode = cursor.getInt(cursor.getColumnIndex("lastPlayListMode"));
                lastPlayMode = cursor.getInt(cursor.getColumnIndex("lastPlayMode"));
                lastMusicId = cursor.getLong(cursor.getColumnIndex("lastMusicId"));
            } while (cursor.moveToNext());
            //拿到数据以后，根据不同的列表找出上次播放歌曲的position
            findPositionFromList();
        } else {
            //首次使用时数据库中还没有数据,先插入一条空数据
            ContentValues values = new ContentValues();
            values.put("infoRecord", "lastMusicInfo");
            values.put("lastPlayListMode", lastPlayListMode);
            values.put("lastPlayMode", lastPlayMode);
            values.put("lastMusicId", lastMusicId);
            //参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
            db.insert(LAST_MUSIC_INFO_TABLE_NAME, null, values);
        }
        cursor.close();

    }

    /**
     * @version V1.0
     * @Title findPositionFromList
     * @author wm
     * @createTime 2023/2/16 17:28
     * @description 根据播放的列表和音乐的ID确定position
     */
    private void findPositionFromList() {
        List<MediaFileBean> tempList = new ArrayList<MediaFileBean>();
        if (lastPlayListMode == 0) {
            tempList = defaultList;
        } else if (lastPlayListMode == 1) {
            tempList = favoriteList;
        }
        if (tempList.size() <= 0) {
            //列表为空，直接返回
            return;
        }
        long tempId;
        for (int i = 0; i < tempList.size(); i++) {
            tempId = tempList.get(i).getId();
            if (tempId == lastMusicId) {
                lastPosition = i;
                //找到了就直接退出循环
                break;
            }
        }
    }

    public List<MediaFileBean> getDefaultList() {
        return defaultList;
    }

    public List<MediaFileBean> getFavoriteList() {
        return favoriteList;
    }

    /**
     * @version V1.0
     * @Title saveFavoriteList
     * @author wm
     * @createTime 2023/2/13 23:02
     * @description 保存收藏列表的信息到数据库中（在app正常关闭流程中调用），若是杀死进程或紧急退出，无法及时保存信息
     */
    public void saveFavoriteList() {
        db.execSQL("DELETE FROM musiclistrecord");

        if (favoriteList.size() > 0) {
            Iterator<MediaFileBean> iterator = favoriteList.iterator();
            while (iterator.hasNext()) {
                MediaFileBean mediaFileBean = iterator.next();
                if (mediaFileBean.isLike()) {
                    ContentValues values = new ContentValues();
                    values.put("musicId", mediaFileBean.getId());
                    values.put("isLike", mediaFileBean.isLike());
                    //参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
                    db.insert(FAVORITE_LIST_TABLE_NAME, null, values);
                }
            }
        }
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
            //添加信息到数据库中
            ContentValues values = new ContentValues();
            values.put("musicId", mediaFileBean.getId());
            values.put("isLike", mediaFileBean.isLike());
            //参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
            db.insert(FAVORITE_LIST_TABLE_NAME, null, values);
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
            //从数据库中删除信息
            db.execSQL("DELETE FROM musiclistrecord WHERE musicId = ?",
                    new Long[]{mediaFileBean.getId()});
        }
        if (defaultList.contains(mediaFileBean)) {
            //需要删除默认列表中的收藏状态,直接操作对象
            mediaFileBean.setLike(false);
        }
    }

    public int getLastPlayListMode() {
        return lastPlayListMode;
    }

    public void setLastPlayListMode(int lastPlayListMode) {
        this.lastPlayListMode = lastPlayListMode;
        updateLastInfo();
    }

    public int getLastPlayMode() {
        return lastPlayMode;
    }

    public void setLastPlayMode(int lastPlayMode) {
        this.lastPlayMode = lastPlayMode;
        updateLastInfo();

    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }

    public void setLastMusicId(long lastMusicId) {
        this.lastMusicId = lastMusicId;
        updateLastInfo();
    }

    /**
     * @param
     * @return
     * @version V1.0
     * @Title updateLastInfo
     * @author wm
     * @createTime 2023/2/16 17:22
     * @description 更新数据库中最后播放的信息，
     */
    private void updateLastInfo() {
        DebugLog.debug("list-mode-id: " + lastPlayListMode + " " + lastPlayMode + " " + lastMusicId);
        ContentValues values = new ContentValues();
        values.put("lastPlayListMode", lastPlayListMode);
        values.put("lastPlayMode", lastPlayMode);
        values.put("lastMusicId", lastMusicId);
        String selection = "infoRecord LIKE ?";
        String[] selectionArgs = {"lastMusicInfo"};

        db.update(
                LAST_MUSIC_INFO_TABLE_NAME,
                values,
                selection,
                selectionArgs);
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
     */
    public static Context getContext() {
        return context;
    }

    public static BasicApplication getApplication() {
        return application;
    }
}

