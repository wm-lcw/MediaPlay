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
    private static HashMap<Long, MediaFileBean> favoriteListMap = new HashMap<>();
    private static HashMap<Long, MediaFileBean> historyListMap = new HashMap<>();
    private static List<MediaFileBean> defaultList = new ArrayList<>();
    private static List<MediaFileBean> favoriteList = new ArrayList<>();
    private static List<MediaFileBean> historyList = new ArrayList<>();
    private static List<MediaFileBean> historyListOut = new ArrayList<>();


    private static String lastPlayListName = Constant.LIST_MODE_DEFAULT_NAME;
    private static int lastPlayMode = 0;
    private static int lastPosition = 0;
    private static long lastMusicId = 0;

    private final static String ALL_MUSIC_LIST_TABLE = "allListsTable";
    private final static String ALL_MUSIC_TABLE = "allMusicTable";
    private final static String LAST_MUSIC_INFO_TABLE = "lastPlayInfoRecord";
    private final static String HISTORY_LIST_TABLE = "historyList";

    private static int defaultListId = 0;
    private static int favoriteListId = 1;

    private static final ExecutorService threadPool = new ThreadPoolExecutor(
            2,
            5,
            2L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(3),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    public DataRefreshService() {}

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

        if (defaultList.size() <= 0){
            // 默认列表为空，则将数据表的内容清空,后续的操作不再执行
            try {
                db.execSQL("DELETE FROM " + ALL_MUSIC_LIST_TABLE);
                db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE);
                db.execSQL("DELETE FROM " + HISTORY_LIST_TABLE);
                db.execSQL("DELETE FROM " + LAST_MUSIC_INFO_TABLE);
                return;
            } catch (Exception exception){
                DebugLog.error("error " + exception.getMessage());
            }
        }

        initDefaultList();
        initFavoriteList();

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
        DebugLog.debug("defaultList size " + defaultList.size());
    }

    /**
     *  初始化默认列表
     *  @author wm
     *  @createTime 2023/9/19 15:40
     */
    @SuppressLint("Recycle")
    private static void initDefaultList() {
        DebugLog.debug("");
        try{
            // 获取默认列表的id
            String query = "SELECT id FROM " + ALL_MUSIC_LIST_TABLE +
                    " WHERE listMode = " + Constant.LIST_SAVE_LIST_MODE_DEFAULT;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                do {
                    defaultListId = cursor.getInt(cursor.getColumnIndex("id"));
                } while (cursor.moveToNext());
            }

            // 先清空默认列表的歌曲，再重新添加一次
            db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE + " WHERE listName = ?",
                    new String[]{Constant.LIST_MODE_DEFAULT_NAME});

            /*
            * 这里要特别注意，使用下面的SQL语句格式，将会报错
            * db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE + " WHERE listName = " + Constant.LIST_MODE_DEFAULT_NAME);
            *
            * 使用下面这种则不会报错
            * db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE + " WHERE listName = ?", new String[]{Constant.LIST_MODE_DEFAULT_NAME});
            *
            * 所以想使用 db.execSQL()方法执行SQL语句时，不要使用SQL语句全拼接的形式，要使用带参数的格式，
            * 或者采用这种分开的写法
            *    String query = "SELECT * FROM " + ALL_MUSIC_LIST_TABLE + " WHERE listMode = " + Constant.LIST_SAVE_LIST_MODE_CUSTOMER;
            *    Cursor cursor = db.rawQuery(query, null);
            * */
            
            for (MediaFileBean mediaFileBean : defaultList) {
                
                // 将默认列表中的音乐放到HashMap中，主要是为了后面筛选自定义列表中的歌曲
                defaultListMap.put(mediaFileBean.getId(), mediaFileBean);

                // 将数据重新写入数据库
                ContentValues values = new ContentValues();
                values.put("musicId", mediaFileBean.getId());
                values.put("musicTitle", mediaFileBean.getTitle());
                values.put("musicArtist", mediaFileBean.getArtist());
                values.put("listName", Constant.LIST_MODE_DEFAULT_NAME);
                values.put("listMode", Constant.LIST_SAVE_LIST_MODE_DEFAULT);
                values.put("listId", defaultListId);
                db.insert(ALL_MUSIC_TABLE, null, values);
            }
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
            clearResource();
        }

    }

    /**
     *  初始化收藏列表
     *  @author wm
     *  @createTime 2023/9/19 15:40
     */
    private static void initFavoriteList(){
        try {
            // 获取收藏列表的id
            String queryId = "SELECT id FROM " + ALL_MUSIC_LIST_TABLE +
                    " WHERE listMode = " + Constant.LIST_SAVE_LIST_MODE_FAVORITE;
            Cursor cursorId = db.rawQuery(queryId, null);
            if (cursorId.moveToFirst()) {
                do {
                    favoriteListId = cursorId.getInt(cursorId.getColumnIndex("id"));
                } while (cursorId.moveToNext());
            }

            // 获取收藏列表的歌曲
            String query = "SELECT * FROM " + ALL_MUSIC_TABLE +
                    " WHERE listMode = " + Constant.LIST_SAVE_LIST_MODE_FAVORITE;
            Cursor cursor = db.rawQuery(query, null);
            // 存在数据才返回true
            if (cursor.moveToFirst()) {
                do {
                    long musicId = cursor.getLong(cursor.getColumnIndex("musicId"));
                    if (defaultListMap.containsKey(musicId)){
                        // 默认列表里存在的才添加到favoriteList中，否则认为是无效数据，需要在数据表中删除该数据
                        favoriteListMap.put(musicId, defaultListMap.get(musicId));
                        favoriteList.add(defaultListMap.get(musicId));
                        defaultListMap.get(musicId).setLike(true);
                    } else {
                        // 删除无效的收藏列表
                        db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE  +
                                        " WHERE listMode = " + Constant.LIST_SAVE_LIST_MODE_FAVORITE + " AND musicId = ?",
                                new Long[]{musicId});
                    }
                } while (cursor.moveToNext());
            }
            cursorId.close();
            cursor.close();
            DebugLog.debug("favoriteListSize " + favoriteList.size());
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
            clearResource();
        }
    }

    /**
     *  根据数据表中数据，创建自定义列表
     *  @author wm
     *  @createTime 2023/9/11 20:08
     */
    private static void createCustomerList() {
        try {
            // 从数据库中获取自定义音乐列表的数据
            String query = "SELECT * FROM " + ALL_MUSIC_LIST_TABLE + " WHERE listMode = " + Constant.LIST_SAVE_LIST_MODE_CUSTOMER;
            Cursor cursor = db.rawQuery(query, null);

            // 遍历音乐列表的数据
            while (cursor.moveToNext()) {
                String listName = cursor.getString(cursor.getColumnIndex("listName"));
                if (!customerListsMap.containsKey(listName)) {
                    // HashSet中没有该列表才新建一个自定义列表对象MusicListBean
                    int listId = cursor.getInt(cursor.getColumnIndex("id"));
                    MusicListBean musicListBean = new MusicListBean(listName);
                    musicListBean.setListId(listId);
                    // 将列表对象添加到自定义列表集合中
                    customerListsMap.put(listName, musicListBean);
                }
            }
            // 关闭游标和数据库连接
            cursor.close();
            DebugLog.debug("customerList size " + customerListsMap);
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
            clearResource();
        }

    }

    /**
     *  初始化各自定义列表
     *  @author wm
     *  @createTime 2023/9/11 20:10
     */
    private static void initCustomerListData() {
        try {
            String query = "SELECT musicId, listName FROM " + ALL_MUSIC_TABLE  +
                    " WHERE listMode = " + Constant.LIST_SAVE_LIST_MODE_CUSTOMER;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String playListName = cursor.getString(cursor.getColumnIndex("listName"));
                    if (!customerListsMap.containsKey(playListName)) {
                        continue;
                    }
                    long musicBeanId = cursor.getLong(cursor.getColumnIndex("musicId"));
                    if (defaultListMap.containsKey(musicBeanId)) {
                        customerListsMap.get(playListName).getMusicList().add(defaultListMap.get(musicBeanId));
                    } else {
                        // 无效数据，需要在数据表中删除该数据
                        db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE +
                                        " WHERE listName = " + playListName + " AND musicId = ?",
                                new Long[]{});
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
        }
    }

    /**
     *  初始化上次播放的信息
     *  @author wm
     *  @createTime 2023/2/15 10:34
     */
    private static void initLastPlayInfo() {
        try {
            // 从上次播放的数据库中获取数据
            Cursor cursor = db.query(LAST_MUSIC_INFO_TABLE, null, null, null, null, null, null);
            // 存在数据才返回true
            if (cursor.moveToFirst()) {
                do {
                    lastPlayListName = cursor.getString(cursor.getColumnIndex("lastPlayListName"));
                    lastPlayMode = cursor.getInt(cursor.getColumnIndex("lastPlayMode"));
                    lastMusicId = cursor.getLong(cursor.getColumnIndex("lastMusicId"));
                } while (cursor.moveToNext());
                // 拿到数据以后，根据不同的列表找出上次播放歌曲的position
                findPositionFromList();
            } else {
                // 首次使用时数据库中还没有数据,先插入一条空数据,否则后面的更新操作无法执行
                ContentValues values = new ContentValues();
                values.put("infoRecord", "lastMusicInfo");
                values.put("lastPlayListName", lastPlayListName);
                values.put("lastPlayMode", lastPlayMode);
                values.put("lastMusicId", lastMusicId);
                // 参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
                db.insert(LAST_MUSIC_INFO_TABLE, null, values);
            }
            cursor.close();
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
        }

    }

    /**
     * @author wm
     * @createTime 2023/2/16 17:28
     * @description 根据播放的列表和音乐的ID确定position
     */
    private static void findPositionFromList() {
        try {
            List<MediaFileBean> tempList = getMusicListByName(lastPlayListName);
            if ((tempList == null) || (tempList.size() <= 0)) {
                // 列表为空，直接返回
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
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
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
            String query = "SELECT * FROM " + HISTORY_LIST_TABLE + " ORDER BY musicRecordId DESC";
            Cursor cursor = db.rawQuery(query, null);
            long musicId = 0;
            // 存在数据才返回true
            if (cursor.moveToFirst()) {
                do {
                    musicId = cursor.getLong(cursor.getColumnIndex("musicId"));
                    if (defaultListMap.containsKey(musicId) && historyList.size() < Constant.HISTORY_LIST_MAX_SIZE){
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
            DebugLog.debug("" + listName + " 列表不存在");
        }
        return musicInfo;

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
        try {
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
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
        }
    }

    /**
     * 更新数据库中最后播放的信息
     *
     * @author wm
     * @createTime 2023/9/3 18:31
     */
    private static void updateLastInfo() {
        try {
            DebugLog.debug("lastPlayListName " + lastPlayListName + "; lastMusicId " + lastMusicId);
            ContentValues values = new ContentValues();
            values.put("lastPlayListName", lastPlayListName);
            values.put("lastPlayMode", lastPlayMode);
            values.put("lastMusicId", lastMusicId);
            String selection = "infoRecord LIKE ?";
            String[] selectionArgs = {"lastMusicInfo"};
            db.update(LAST_MUSIC_INFO_TABLE, values, selection, selectionArgs);
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
        }

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
            if (!customerListsMap.containsKey(listName)) {
                threadPool.execute(() -> {
                    DebugLog.debug("insert new list " + listName);
                    // 插入数据
                    ContentValues values = new ContentValues();
                    values.put("listName", listName);
                    values.put("listMode", Constant.LIST_SAVE_LIST_MODE_CUSTOMER);
                    long listId = db.insert(ALL_MUSIC_LIST_TABLE, null, values);

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
                    sendCustomerListsChangeBroadcast(listName, Constant.CUSTOMER_LIST_OPERATOR_CREATE);
                });
            }
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
            if (customerListsMap.containsKey(listName)) {
                threadPool.execute(() -> {
                    DebugLog.debug("delete list " + listName);
                    db.execSQL("DELETE FROM " + ALL_MUSIC_LIST_TABLE + " WHERE listName = ?",
                            new String[]{listName});
                    customerListsMap.remove(listName);
                    sendCustomerListsChangeBroadcast(listName, Constant.CUSTOMER_LIST_OPERATOR_DELETE);
                });
            }
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
            if (!customerListsMap.containsKey(listName)) {
                return;
            }
            threadPool.execute(() -> {
                long listId = customerListsMap.get(listName).getListId();
                List<MediaFileBean> tempList = customerListsMap.get(listName).getMusicList();

                for (long musicId : musicList) {
                    if (!defaultListMap.containsKey(musicId)) {
                        continue;
                    }
                    if (tempList.contains(defaultListMap.get(musicId))) {
                        continue;
                    }
                    // 默认列表存在该歌曲，且列表中还未存在该歌曲才插入数据
                    ContentValues values = new ContentValues();
                    values.put("musicId", musicId);
                    values.put("musicTitle", defaultListMap.get(musicId).getTitle());
                    values.put("musicArtist", defaultListMap.get(musicId).getArtist());
                    values.put("listName", listName);
                    values.put("listMode", Constant.LIST_SAVE_LIST_MODE_CUSTOMER);
                    values.put("listId", listId);
                    db.insert(ALL_MUSIC_TABLE, null, values);
                    tempList.add(defaultListMap.get(musicId));
                }
                sendMusicChangeBroadcast(listName, Constant.CUSTOMER_MUSIC_OPERATOR_INSERT, false, -1);
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
            if (!customerListsMap.containsKey(listName)) {
                return;
            }
            threadPool.execute(() -> {
                boolean deleteMusic = false;
                if (lastPlayListName.equalsIgnoreCase(listName) && musicList.contains(lastMusicId)) {
                    // 删除的歌曲里面包含当前正在播放的歌曲，则需要让MainActivity停止播放，再进行删除操作
                    Intent intent = new Intent(Constant.STOP_PLAY_CUSTOMER_MUSIC_ACTION);
                    context.sendBroadcast(intent);
                    deleteMusic = true;
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        DebugLog.debug("InterruptedException error " + e.getMessage());
                    }
                }
                for (long musicId : musicList) {
                    if (customerListsMap.get(listName).getMusicList().contains(defaultListMap.get(musicId))) {
                        // 列表中存在该歌曲才删除数据
                        customerListsMap.get(listName).getMusicList().remove(defaultListMap.get(musicId));
                        db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE +
                                        " WHERE listName = " + listName + " AND musicId = ?",
                                new Long[]{musicId});
                    }
                }
                sendMusicChangeBroadcast(listName, Constant.CUSTOMER_MUSIC_OPERATOR_DELETE, deleteMusic, lastMusicId);
            });
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
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
        try {
            if (!favoriteList.contains(mediaFileBean) && defaultList.contains(mediaFileBean)) {
                mediaFileBean.setLike(true);
                favoriteList.add(mediaFileBean);
                favoriteListMap.put(mediaFileBean.getId(), mediaFileBean);
                //添加信息到数据库中
                ContentValues values = new ContentValues();

                values.put("musicId", mediaFileBean.getId());
                values.put("musicTitle", mediaFileBean.getTitle());
                values.put("musicArtist", mediaFileBean.getArtist());
                values.put("listName", Constant.LIST_MODE_FAVORITE_NAME);
                values.put("listMode", Constant.LIST_SAVE_LIST_MODE_FAVORITE);
                values.put("listId", favoriteListId);
                //参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
                db.insert(ALL_MUSIC_TABLE, null, values);
                sendMusicChangeBroadcast(Constant.LIST_MODE_FAVORITE_NAME, Constant.CUSTOMER_MUSIC_OPERATOR_INSERT, false, lastMusicId);
            }
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
        }

    }

    /**
     * 取消收藏音乐,这个方法仅在播放页点击取消收藏时调用
     * 此时的列表不一定就是收藏列表,所以需要列表名来判断，当前取消收藏的列表是否是收藏列表
     * @param listName: 取消收藏的列表
     * @param mediaFileBean: 取消收藏的音乐对象
     * @author wm
     * @createTime 2023/9/3 16:57
     */
    public static void removeFavoriteMusic(String listName, MediaFileBean mediaFileBean) {
        try {
            if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(listName)){
                // 如果当前播放的是收藏列表，可以直接调用deleteFavoriteMusic来处理
                List<Long> list = new ArrayList<>();
                list.add(mediaFileBean.getId());
                deleteFavoriteMusic(listName,list);
            } else {
                // 否则直接删除收藏列表中的歌曲，然后刷新UI即可
                if (favoriteList.contains(mediaFileBean) && defaultList.contains(mediaFileBean)) {
///                    int position = favoriteList.indexOf(mediaFileBean);
///                    favoriteList.get(position).setLike(false);
                    mediaFileBean.setLike(false);
                    favoriteList.remove(mediaFileBean);
                    favoriteListMap.remove(mediaFileBean.getId());
                    // 从数据库中删除信息
                    db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE  + " WHERE musicId = ? " +
                                    " AND listName = " + Constant.LIST_MODE_FAVORITE_NAME,
                            new Long[]{mediaFileBean.getId()});
                }
                sendMusicChangeBroadcast(listName, Constant.CUSTOMER_MUSIC_OPERATOR_DELETE, false, lastMusicId);
            }
        } catch (Exception exception){
            DebugLog.error("error " + exception.getMessage());
        }
    }

    /**
     *  删除收藏列表的歌曲
     *  @author wm
     *  @createTime 2023/9/13 22:49
     * @param listName:
     * @param musicList:
     */
    public static void deleteFavoriteMusic(String listName, List<Long> musicList) {
        try {
            threadPool.execute(() -> {
                DebugLog.debug("list contains id " + musicList.contains(lastMusicId));
                boolean deleteMusic = false;
                if (lastPlayListName.equalsIgnoreCase(listName) && musicList.contains(lastMusicId)) {
                    // 删除的歌曲里面包含当前正在播放的歌曲，则需要让MainActivity停止播放，再进行删除操作
                    Intent intent = new Intent(Constant.STOP_PLAY_CUSTOMER_MUSIC_ACTION);
                    context.sendBroadcast(intent);
                    deleteMusic = true;
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        DebugLog.debug("InterruptedException error " + e.getMessage());
                    }
                }
                for (long musicId : musicList) {
                    if (favoriteListMap.containsKey(musicId)) {
                        // 验证修改favoriteList中的MusicBean对象是否可以通用
                        favoriteListMap.get(musicId).setLike(false);
                        favoriteList.remove(favoriteListMap.get(musicId));
                        favoriteListMap.remove(musicId);
                        // 从数据库中删除信息
                        db.execSQL("DELETE FROM " + ALL_MUSIC_TABLE  + " WHERE musicId = ? " +
                                        " AND listName = " + Constant.LIST_MODE_FAVORITE_NAME,
                                new Long[]{musicId});
                    }
                }
                sendMusicChangeBroadcast(listName, Constant.CUSTOMER_MUSIC_OPERATOR_DELETE, deleteMusic, lastMusicId);
            });
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }
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
                db.execSQL("DELETE FROM " + HISTORY_LIST_TABLE + " WHERE musicId = ?",
                        new Long[]{musicId});
            }
            // 数据库写入历史播放记录
            ContentValues values = new ContentValues();
            values.put("musicId", musicId);
            db.insert(HISTORY_LIST_TABLE, null, values);
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
                db.execSQL("DELETE FROM " + HISTORY_LIST_TABLE + " WHERE musicId = ?",
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
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        DebugLog.debug("error " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                for (long musicId : deleteMusicList) {
                    if (historyListMap.containsKey(musicId)) {
                        db.execSQL("DELETE FROM " + HISTORY_LIST_TABLE + " WHERE musicId = ?",
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
            DebugLog.debug("listName " + listName + "; position " + position);
            if (customerListsMap.containsKey(listName)) {
                // 自定义列表单独处理
                List<Long> list = new ArrayList<>();
                list.add(customerListsMap.get(listName).getMusicList().get(position).getId());
                // 增删自定义列表的操作里面有发广播通知MainActivity更新UI和处理逻辑
                deleteMultipleMusic(listName, list);
            } else if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(listName)) {
                List<Long> list = new ArrayList<>();
                list.add(favoriteList.get(position).getId());
                deleteMultipleMusic(listName, list);
            } else if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(listName)) {
                List<Long> list = new ArrayList<>();
                list.add(historyList.get(position).getId());
                deleteMultipleMusic(listName, list);
            }
        } catch (Exception exception) {
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /**
     * 删除多首音乐,默认列表不能执行删除操作
     *
     * @param listName: 列表名
     * @param musicList: 要删除音乐的id集合
     * @author wm
     * @createTime 2023/9/3 17:03
     */
    public static void deleteMultipleMusic(String listName, List<Long> musicList) {
        try {
            if (customerListsMap.containsKey(listName)){
                deleteCustomerMusic(listName, musicList);
            } else if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(listName)){
                deleteFavoriteMusic(listName, musicList);
            } else if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(listName)) {
                deleteMultipleHistoryMusic(musicList);
            }
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

}