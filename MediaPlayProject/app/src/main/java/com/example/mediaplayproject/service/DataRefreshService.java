package com.example.mediaplayproject.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;

import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.bean.MusicListBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MusicDataBaseHelper;
import com.example.mediaplayproject.utils.SearchFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wm
 */
public class DataRefreshService extends Service {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static SQLiteDatabase db;

    private static HashMap<String, MusicListBean> customerListsMap = new HashMap<>();
    private static HashMap<Long, MediaFileBean> defaultListMap = new HashMap<>();
    private static HashMap<Long, Integer> favoriteListMap = new HashMap<>();
    private static HashMap<Long, MediaFileBean> historyListMap = new HashMap<>();
    private static List<MediaFileBean> defaultList = new ArrayList<>();
    private static List<MediaFileBean> favoriteList = new ArrayList<>();
    private static List<MediaFileBean> historyList = new ArrayList<>();
    private static List<MediaFileBean> historyListOut = new ArrayList<>();
    private static final int HISTORY_LIST_MAX_SIZE = 50;

    private static String lastPlayListName = Constant.LIST_MODE_DEFAULT_NAME;
    private static int lastPlayMode = 0;
    private static int lastPosition = 0;
    private static long lastMusicId = 0;

    private final static String FAVORITE_LIST_TABLE_NAME = "favoriteList";
    private final static String LAST_MUSIC_INFO_TABLE_NAME = "lastPlayInfoRecord";
    private final static String HISTORY_LIST_TABLE_NAME = "historyList";
    private final static String CUSTOMER_LIST_TABLE_NAME = "Playlist";
    private final static String CUSTOMER_MUSIC_TABLE_NAME = "Music";

    private static ExecutorService threadPool = new ThreadPoolExecutor(
            2,
            5,
            2L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(3),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );


    public DataRefreshService() {
    }

    @Override
    public void onCreate() {
        DebugLog.debug("");
        super.onCreate();
        context = getApplicationContext();
        MusicDataBaseHelper musicDataBaseHelper = new MusicDataBaseHelper(context, "musicplay.db", null, 1);
        db = musicDataBaseHelper.getReadableDatabase();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 初始化音乐资源
     *
     * @author wm
     * @createTime 2023/8/24 18:15
     */
    public static void initResource() {
        clearResource();
        searchMusic();

        createCustomerList();
        initCustomerListData();

        initLastPlayInfo();
        initHistoryList();

    }

    /**
     *  初始化之前清空列表资源
     *  @author wm
     *  @createTime 2023/9/11 20:08
     */
    public static void clearResource() {
        customerListsMap.clear();
        favoriteListMap.clear();
        historyListMap.clear();
        defaultList.clear();
        favoriteList.clear();
        historyList.clear();
        historyListOut.clear();
    }

    /**
     *  搜索设备里的音乐媒体文件
     *  @author wm
     *  @createTime 2023/2/11 15:46
     */
    private static void searchMusic() {
        SearchFiles mSearcherFiles = SearchFiles.getInstance(context);
        // 搜索设备里的所有音乐媒体文件，存储到默认列表中（默认列表就是设备里的所有音乐文件）
        defaultList = mSearcherFiles.getMusicInfo();

        Cursor cursor = db.query(FAVORITE_LIST_TABLE_NAME, null, null, null, null, null, null);
        //存在数据才返回true
        if (cursor.moveToFirst()) {
            do {
                long musicId = cursor.getLong(cursor.getColumnIndex("musicId"));
                int isLike = cursor.getInt(cursor.getColumnIndex("isLike"));
                favoriteListMap.put(musicId, isLike);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // favoriteListMap与默认列表对比，找出存在且收藏的歌，将其添加到收藏列表中
        long id;
        for (int i = 0; i < defaultList.size(); i++) {
            id = defaultList.get(i).getId();
            if (favoriteListMap.containsKey(id) && favoriteListMap.get(id) != null) {
                defaultList.get(i).setLike(1 == favoriteListMap.get(id));
                if (!favoriteList.contains(defaultList.get(i))) {
                    favoriteList.add(defaultList.get(i));
                }
            }
        }

        // 将默认列表中的音乐放到HashMap中，主要是为了后面筛选自定义列表中的歌曲
        if (defaultList.size() > 0) {
            Iterator<MediaFileBean> iterator = defaultList.iterator();
            while (iterator.hasNext()) {
                MediaFileBean mediaFileBean = iterator.next();
                defaultListMap.put(mediaFileBean.getId(), mediaFileBean);
            }
        }

        DebugLog.debug("-- favoriteListSize " + favoriteList.size());
    }

    /**
     *  根据自定义列表的数据表中的内容，创建自定义列表
     *  @author wm
     *  @createTime 2023/9/11 20:08
     */
    private static void createCustomerList() {
        // 使用SQL查询语句从数据库中获取音乐列表的数据
        String query = "SELECT * FROM " + CUSTOMER_LIST_TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);

        // 遍历查询结果的游标cursor，获取每一条音乐列表的数据
        while (cursor.moveToNext()) {
            String listName = cursor.getString(cursor.getColumnIndex("list_name"));
            if (!customerListsMap.containsKey(listName)) {
                // HashSet中没有该列表才新建一个MusicListBean
                // 根据每一条音乐列表的数据，创建对应的列表对象
                MusicListBean musicListBean = new MusicListBean(listName);
                int listId = cursor.getInt(cursor.getColumnIndex("id"));
                musicListBean.setListId(listId);
                // 将列表对象添加到动态列表中
                customerListsMap.put(listName, musicListBean);
            }
        }
        // 关闭游标和数据库连接
        cursor.close();
    }

    /**
     *  初始化各自定义列表
     *  @author wm
     *  @createTime 2023/9/11 20:10
     */
    private static void initCustomerListData() {
        String query = "SELECT Music.music_bean_id, Playlist.list_name FROM Music " +
                "INNER JOIN Playlist ON Music.play_list_id = Playlist.id";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String playListName = cursor.getString(cursor.getColumnIndex("list_name"));
                if (customerListsMap.containsKey(playListName)) {
                    long musicBeanId = cursor.getLong(cursor.getColumnIndex("music_bean_id"));
                    if (defaultListMap.containsKey(musicBeanId)) {
                        customerListsMap.get(playListName).getMusicList().add(defaultListMap.get(musicBeanId));
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        getCustomerList();
    }



    /**
     *  初始化上次播放的信息
     *  @author wm
     *  @createTime 2023/2/15 10:34
     */
    private static void initLastPlayInfo() {
        // 从上次播放的数据库中获取数据
        Cursor cursor = db.query(LAST_MUSIC_INFO_TABLE_NAME, null, null, null, null, null, null);
        // 存在数据才返回true
        if (cursor.moveToFirst()) {
            do {
                lastPlayListName = cursor.getString(cursor.getColumnIndex("lastPlayListName"));
                lastPlayMode = cursor.getInt(cursor.getColumnIndex("lastPlayMode"));
                lastMusicId = cursor.getLong(cursor.getColumnIndex("lastMusicId"));
            } while (cursor.moveToNext());
            //拿到数据以后，根据不同的列表找出上次播放歌曲的position
            findPositionFromList();
        } else {
            //首次使用时数据库中还没有数据,先插入一条空数据
            ContentValues values = new ContentValues();
            values.put("infoRecord", "lastMusicInfo");
            values.put("lastPlayListName", lastPlayListName);
            values.put("lastPlayMode", lastPlayMode);
            values.put("lastMusicId", lastMusicId);
            //参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
            db.insert(LAST_MUSIC_INFO_TABLE_NAME, null, values);
        }
        cursor.close();
    }

    /**
     * @author wm
     * @createTime 2023/2/16 17:28
     * @description 根据播放的列表和音乐的ID确定position
     */
    private static void findPositionFromList() {
        List<MediaFileBean> tempList = getMusicListByName(lastPlayListName);
        if ((tempList == null) || (tempList.size() <= 0)) {
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

    /**
     *  初始化历史播放列表
     *  @author wm
     *  @createTime 2023/9/11 20:13
     */
    private static void initHistoryList() {
        try {
            historyListMap.clear();
            historyList.clear();
            // 从历史播放的数据表中获取数据
            String query = "SELECT * FROM " + HISTORY_LIST_TABLE_NAME + " ORDER BY musicRecordId DESC";
            Cursor cursor = db.rawQuery(query, null);
            long musicId = 0;
            // 存在数据才返回true
            if (cursor.moveToFirst()) {
                do {
                    musicId = cursor.getLong(cursor.getColumnIndex("musicId"));
                    if (defaultListMap.containsKey(musicId) && historyList.size() < HISTORY_LIST_MAX_SIZE){
                        // 默认列表中存在该音乐，且历史列表不超过50，才添加进入历史播放列表里
                        historyListMap.put(musicId, defaultListMap.get(musicId));
                        historyList.add(defaultListMap.get(musicId));
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception exception){
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    public static String getLastPlayListName() {
        return lastPlayListName;
    }

    public static int getLastPlayMode() {
        return lastPlayMode;
    }

    public static int getLastPosition() {
        return lastPosition;
    }

    public static long getLastMusicId() {
        return lastMusicId;
    }

    /**
     * 保存上次播放的音乐信息
     *
     * @param lastPlayListName: 列表名
     * @param lastPosition:     音乐下标
     * @param lastMusicId:      音乐的id
     * @param lastPlayMode:     播放模式
     * @author wm
     * @createTime 2023/9/3 18:31
     */
    public static void setLastPlayInfo(String lastPlayListName, int lastPosition, long lastMusicId, int lastPlayMode) {
        DebugLog.debug("thisId " + DataRefreshService.lastMusicId + "; input id "+ lastMusicId);
        boolean isListNameSame = DataRefreshService.lastPlayListName.equalsIgnoreCase(lastPlayListName);
        boolean isPositionSame = DataRefreshService.lastPosition == lastPosition;
        boolean isMusicIdSame = DataRefreshService.lastMusicId == lastMusicId;
        boolean isPlayModeSame = DataRefreshService.lastPlayMode == lastPlayMode;
        if (isListNameSame && isPositionSame && isMusicIdSame && isPlayModeSame) {
            // 有数据更改时才更新，否则不处理
            DebugLog.debug("no deal");
        } else {
            DataRefreshService.lastPlayListName = lastPlayListName;
            if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(lastPlayListName)){
                // 最近播放列表保存的永远是最后一条记录
                DataRefreshService.lastPosition = 0;
            } else {
                DataRefreshService.lastPosition = lastPosition;
            }

            DataRefreshService.lastMusicId = lastMusicId;
            DataRefreshService.lastPlayMode = lastPlayMode;
            updateLastInfo();
        }
        DebugLog.debug("refresh music idChange ? " + isMusicIdSame);
        if (!isMusicIdSame && defaultListMap.containsKey(lastMusicId)){
            // 只根据id去判定历史播放记录，id更改才刷新播放记录
            addHistoryMusic(defaultListMap.get(lastMusicId));
        }
    }

    /**
     * 更新数据库中最后播放的信息
     *
     * @author wm
     * @createTime 2023/9/3 18:31
     */
    private static void updateLastInfo() {

        ContentValues values = new ContentValues();
        values.put("lastPlayListName", lastPlayListName);
        values.put("lastPlayMode", lastPlayMode);
        values.put("lastMusicId", lastMusicId);
        String selection = "infoRecord LIKE ?";
        String[] selectionArgs = {"lastMusicInfo"};
        db.update(LAST_MUSIC_INFO_TABLE_NAME, values, selection, selectionArgs);
    }

    public static List<MediaFileBean> getDefaultList() {
        return defaultList;
    }

    public static List<MediaFileBean> getFavoriteList() {
        return favoriteList;
    }

    public static List<MediaFileBean> getHistoryList() {
        return historyList;
    }

    public static List<MusicListBean> getCustomerList() {
        List<MusicListBean> list = new ArrayList<>();
        try {
            for (Map.Entry<String, MusicListBean> entry : customerListsMap.entrySet()) {
                MusicListBean musicListBean = entry.getValue();
                list.add(musicListBean);
            }
            DebugLog.debug("lists size " + list.size());
            return list;
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
            return null;
        }
    }

    /**
     * 将歌曲加入收藏列表
     *
     * @param mediaFileBean: 添加收藏的音乐对象
     * @author wm
     * @createTime 2023/9/3 16:57
     */
    public static void addMusicToFavoriteList(MediaFileBean mediaFileBean) {
        DebugLog.debug("mediaFileBean " + mediaFileBean);
        if (!favoriteList.contains(mediaFileBean)) {
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
     * 取消收藏音乐
     *
     * @param mediaFileBean: 取消收藏的音乐对象
     * @author wm
     * @createTime 2023/9/3 16:57
     */
    public static void removeFavoriteMusic(MediaFileBean mediaFileBean) {
        DebugLog.debug("mediaFileBean " + mediaFileBean);
        boolean deletePlayingMusic = false;
        if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(lastPlayListName) && mediaFileBean.getId() == lastMusicId){
            // 删除的歌曲里面包含当前正在播放的歌曲，则需要让MainActivity停止播放，再进行删除操作
            Intent intent = new Intent(Constant.STOP_PLAY_CUSTOMER_MUSIC_ACTION);
            context.sendBroadcast(intent);
            deletePlayingMusic = true;
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                DebugLog.debug("error " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (favoriteList.contains(mediaFileBean) ) {
            favoriteList.remove(mediaFileBean);
            //从数据库中删除信息
            db.execSQL("DELETE FROM favoriteList WHERE musicId = ?",
                    new Long[]{mediaFileBean.getId()});
            if (defaultList.contains(mediaFileBean)) {
                // 需要删除默认列表中的收藏状态,直接操作对象
                mediaFileBean.setLike(false);
            }
        }
        sendMusicChangeBroadcast(Constant.LIST_MODE_FAVORITE_NAME, Constant.CUSTOMER_MUSIC_OPERATOR_DELETE, deletePlayingMusic, lastMusicId);
    }

    /**
     * 创建自定义音乐列表
     *
     * @param listName: 列表名
     * @author wm
     * @createTime 2023/9/3 17:01
     */
    public static void createNewCustomerMusicList(String listName) {
        try {
            threadPool.execute(() -> {
                DebugLog.debug("map contains list : " + customerListsMap.containsKey(listName));
                if (!customerListsMap.containsKey(listName)) {
                    DebugLog.debug("insert new list " + listName);
                    // 插入数据
                    ContentValues values = new ContentValues();
                    values.put("list_name", listName);
                    // 参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
                    long listId = db.insert(CUSTOMER_LIST_TABLE_NAME, null, values);

                    // 获取刚插入的自动递增id
                    Cursor cursor = db.rawQuery("SELECT last_insert_rowid() AS id", null);
                    if (cursor != null && cursor.moveToFirst()) {
                        listId = cursor.getLong(cursor.getColumnIndex("id"));
                    }
                    if (listId != 0) {
                        MusicListBean musicListBean = new MusicListBean(listName);
                        musicListBean.setListId(listId);
                        // 将列表对象添加到动态列表中
                        customerListsMap.put(listName, musicListBean);
                    }
                }
                sendCustomerListsChangeBroadcast(listName, Constant.CUSTOMER_LIST_OPERATOR_CREATE);
            });

        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /**
     * 删除自定义列表
     *
     * @param listName: 要删除列表的列表名
     * @author wm
     * @createTime 2023/9/3 17:01
     */
    public static void deleteCustomerMusicList(String listName) {
        try {
            threadPool.execute(() -> {
                if (customerListsMap.containsKey(listName)) {
                    DebugLog.debug("delete list " + listName);
                    db.execSQL("DELETE FROM " + CUSTOMER_LIST_TABLE_NAME + " WHERE list_name = ?",
                            new String[]{listName});
                    customerListsMap.remove(listName);
                    DebugLog.debug("listSize After delete " + customerListsMap.size());
                    sendCustomerListsChangeBroadcast(listName, Constant.CUSTOMER_LIST_OPERATOR_DELETE);
                }
            });
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /**
     * 往自定义列表中插入歌曲，需要满足多个条件才插入：
     * 列表存在、默认列表中包含插入的歌曲、自定义列表中未包含该歌曲
     *
     * @param listName:  列表名
     * @param musicList: 要插入的列表
     * @author wm
     * @createTime 2023/8/29 22:35
     */
    public static void insertCustomerMusic(String listName, List<Long> musicList) {
        try {
            threadPool.execute(() -> {
                DebugLog.debug("listName " + listName + "; musicList " + musicList);
                if (customerListsMap.containsKey(listName)) {
                    DebugLog.debug("exist list -- " + listName);
                    long listId = customerListsMap.get(listName).getListId();
                    for (long musicId : musicList) {
                        if (defaultListMap.containsKey(musicId)) {
                            if (!customerListsMap.get(listName).getMusicList().contains(defaultListMap.get(musicId))) {
                                // 列表中还未存在该歌曲才插入数据
                                ContentValues values = new ContentValues();
                                values.put("music_bean_id", musicId);
                                values.put("play_list_id", listId);
                                //参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
                                db.insert(CUSTOMER_MUSIC_TABLE_NAME, null, values);
                                customerListsMap.get(listName).getMusicList().add(defaultListMap.get(musicId));
                            }
                        }
                    }
                    sendMusicChangeBroadcast(listName, Constant.CUSTOMER_MUSIC_OPERATOR_INSERT, false, -1);
                }
            });
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /**
     * 删除自定义列表中的歌曲
     *
     * @param listName:  列表名
     * @param musicList: 要删除的音乐集合
     * @author wm
     * @createTime 2023/9/3 17:02
     */
    public static void deleteCustomerMusic(String listName, List<Long> musicList) {
        try {
            threadPool.execute(() -> {
                DebugLog.debug("list contains id " + musicList.contains(lastMusicId));
                boolean deleteMusic = false;
                if (customerListsMap.containsKey(listName)) {
                    if (lastPlayListName.equalsIgnoreCase(listName) && musicList.contains(lastMusicId)) {
                        // 删除的歌曲里面包含当前正在播放的歌曲，则需要让MainActivity停止播放，再进行删除操作
                        Intent intent = new Intent(Constant.STOP_PLAY_CUSTOMER_MUSIC_ACTION);
                        context.sendBroadcast(intent);
                        deleteMusic = true;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            DebugLog.debug("error " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    for (long musicId : musicList) {
                        if (defaultListMap.containsKey(musicId)) {
                            if (customerListsMap.get(listName).getMusicList().contains(defaultListMap.get(musicId))) {
                                // 列表中存在该歌曲才能删除数据
                                customerListsMap.get(listName).getMusicList().remove(defaultListMap.get(musicId));
                                db.execSQL("DELETE FROM " + CUSTOMER_MUSIC_TABLE_NAME + " WHERE music_bean_id = ?",
                                        new Long[]{musicId});
                            }
                        }
                    }
                    sendMusicChangeBroadcast(listName, Constant.CUSTOMER_MUSIC_OPERATOR_DELETE, deleteMusic, lastMusicId);
                }
            });
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /**
     * 通过列表名获取列表
     *
     * @param listName: 列表名
     * @return : java.util.List<com.example.mediaplayproject.bean.MediaFileBean>
     * @author wm
     * @createTime 2023/9/3 17:02
     */
    public static List<MediaFileBean> getMusicListByName(String listName) {
        List<MediaFileBean> musicInfo;
        if (Constant.LIST_MODE_DEFAULT_NAME.equalsIgnoreCase(listName)) {
            musicInfo = defaultList;
        } else if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(listName)) {
            musicInfo = favoriteList;
        } else if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(listName)) {
            musicInfo = historyList;
        } else if (customerListsMap.containsKey(listName)) {
            musicInfo = customerListsMap.get(listName).getMusicList();
        } else {
            musicInfo = null;
            DebugLog.debug("名称为”" + listName + "“的列表不存在");
        }
        return musicInfo;

    }

    /**
     * 删除单首音乐,默认列表不能执行删除操作
     *
     * @param listName: 列表名
     * @param position: 要删除音乐的下标
     * @author wm
     * @createTime 2023/9/3 17:03
     */
    public static void deleteMusic(String listName, int position) {
        try {
            threadPool.execute(() -> {
                DebugLog.debug("listName " + listName + "; position " + position);
                if (customerListsMap.containsKey(listName)){
                    // 自定义列表单独处理
                    List<Long> list = new ArrayList<>();
                    list.add(customerListsMap.get(listName).getMusicList().get(position).getId());
                    // 增删自定义列表的操作里面有发广播通知MainActivity更新UI和处理逻辑
                    deleteCustomerMusic(listName, list);
                } else if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(listName)){
                    removeFavoriteMusic(favoriteList.get(position));
                } else if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(listName)){
                    // deleteHistoryMusic(historyList.get(position));
                }
            });
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }
    }


    /**
     * 发送自定义列表更新的广播
     *
     * @param operation: 列表操作类型
     * @author wm
     * @createTime 2023/9/4 18:30
     */
    private static void sendCustomerListsChangeBroadcast(String listName, int operation) {
        Intent intent = new Intent(Constant.OPERATE_CUSTOMER_MUSIC_LIST_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt("listOperation", operation);
        bundle.putString("listName", listName);
        intent.putExtras(bundle);
        context.sendBroadcast(intent);
    }

    /**
     *  发送列表歌曲数量变化的广播,使用于收藏列表、自定义列表，后续验证是否适用于最近播放列表
     *  @author wm
     *  @createTime 2023/9/11 20:34
     * @param listName:列表名
     * @param operation: 变化的操作--3：插入； 4：删除；
     * @param deletePlayingMusic: 删除的歌曲是否当前播放的歌曲；
     * @param musicId: 删除音乐的ID
     */
    private static void sendMusicChangeBroadcast(String listName, int operation, boolean deletePlayingMusic, long musicId) {
        Intent intent = new Intent(Constant.OPERATE_MUSIC_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt("listOperation", operation);
        bundle.putBoolean("deletePlayingMusic", deletePlayingMusic);
        bundle.putLong("musicId", musicId);
        bundle.putString("listName", listName);
        intent.putExtras(bundle);
        context.sendBroadcast(intent);
    }

    /**
     *  增加历史播放歌曲
     *  @author wm
     *  @createTime 2023/9/11 20:36
     * @param mediaFileBean: 新增加的歌曲信息
     */
    public static void addHistoryMusic(MediaFileBean mediaFileBean) {
        try{
            long musicId = mediaFileBean.getId();
            DebugLog.debug(" id " + musicId);
            if (historyListMap.containsKey(musicId)) {
                // 删除数据库里的内容
                db.execSQL("DELETE FROM " + HISTORY_LIST_TABLE_NAME + " WHERE musicId = ?",
                        new Long[]{musicId});
            }
            // 数据库写入历史播放记录
            ContentValues values = new ContentValues();
            values.put("musicId", musicId);
            db.insert(HISTORY_LIST_TABLE_NAME, null, values);
            initHistoryList();
        } catch (Exception exception){
            DebugLog.debug("error "+ exception.getMessage());
        }

    }

    /**
     *  删除历史播放列表中的歌曲
     *  @author wm
     *  @createTime 2023/9/11 20:37
     * @param mediaFileBean: 要删除的歌曲信息
     */
    public static void deleteHistoryMusic(MediaFileBean mediaFileBean){
        try {
            long musicId = mediaFileBean.getId();
            if (historyListMap.containsKey(musicId)) {
                // 删除数据库里的内容
                db.execSQL("DELETE FROM " + HISTORY_LIST_TABLE_NAME + " WHERE musicId = ?",
                        new Long[]{musicId});
            }
            initHistoryList();
        } catch (Exception exception){
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /**
     *  删除多首历史播放列表的歌曲
     *  @author wm
     *  @createTime 2023/9/11 21:10
     * @param deleteMusicList: 要删除的音乐Id集合
     */
    public static void deleteMultipleHistoryMusic(List<Long> deleteMusicList) {
        try {
            threadPool.execute(() -> {
                boolean deleteMusic = false;
                if (lastPlayListName.equalsIgnoreCase(Constant.LIST_MODE_HISTORY_NAME) && deleteMusicList.contains(lastMusicId)) {
                    // 删除的歌曲里面包含当前正在播放的歌曲，则需要让MainActivity停止播放，再进行删除操作
                    Intent intent = new Intent(Constant.STOP_PLAY_CUSTOMER_MUSIC_ACTION);
                    context.sendBroadcast(intent);
                    deleteMusic = true;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        DebugLog.debug("error " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                for (long musicId : deleteMusicList) {
                    if (historyListMap.containsKey(musicId)) {
                        db.execSQL("DELETE FROM " + HISTORY_LIST_TABLE_NAME + " WHERE musicId = ?",
                                new Long[]{musicId});
                    }
                }
                initHistoryList();
                sendMusicChangeBroadcast(Constant.LIST_MODE_HISTORY_NAME, Constant.CUSTOMER_MUSIC_OPERATOR_DELETE, deleteMusic, lastMusicId);

            });
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /*
    * 列表里单点的删除操作
    *   判断是自定义列表--调用删除多首自定义列表音乐的逻辑处理--处理完之后会发一个OPERATE_CUSTOMER_MUSIC_ACTION广播
    *       给MainActivity处理停止播放、切换播放和刷新UI等操作
    *   判断是收藏列表--调用删除收藏列表音乐的逻辑处理--这里没有发送广播给MainActivity刷新信息
    *   判断是最近播放列表-是否可以和自定义列表类似的操作，处理完之后会发一个OPERATE_CUSTOMER_MUSIC_ACTION广播等
    *   其他的列表不操作
    *
    *
    *
    * */


}