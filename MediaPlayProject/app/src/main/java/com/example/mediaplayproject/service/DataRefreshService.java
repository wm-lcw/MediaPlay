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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author wm
 */
public class DataRefreshService extends Service {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static SQLiteDatabase db;

    /**
     * 自定义音乐列表的集合
     */
    private static HashMap<String, MusicListBean> customerListsMap = new HashMap<>();
    /**
     * 默认列表的集合
     */
    private static HashMap<Long, MediaFileBean> defaultListMap = new HashMap<>();

    /**
     * 从数据库中获取的收藏列表标记
     */
    private static HashMap<Long, Integer> musicListFromData = new HashMap<>();
    /**
     * 默认的列表
     */
    private static List<MediaFileBean> defaultList = new ArrayList<>();
    /**
     * 收藏的列表
     */
    private static List<MediaFileBean> favoriteList = new ArrayList<>();

    private static int lastPlayListMode = 0;
    private static int lastPlayMode = 0;
    private static int lastPosition = 0;
    private static long lastMusicId = 0;

    private final static String FAVORITE_LIST_TABLE_NAME = "musiclistrecord";

    private final static String LAST_MUSIC_INFO_TABLE_NAME = "lastplayinforecord";

    private final static String CUSTOMER_LIST_TABLE_NAME = "Playlist";
    private final static String CUSTOMER_MUSIC_TABLE_NAME = "Music";

    public DataRefreshService() {
    }

    @Override
    public void onCreate() {
        DebugLog.debug("");
        super.onCreate();
        context = getApplicationContext();
        MusicDataBaseHelper musicDataBaseHelper = new MusicDataBaseHelper(context, "musicplay.db", null, 1);
        db = musicDataBaseHelper.getReadableDatabase();
        DebugLog.debug("db : " + db);
//        initResource();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DebugLog.debug("-");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DebugLog.debug("---");
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

        // 初始化上次播放的相关信息
        initLastPlayInfo();
    }


    public static void clearResource() {
        customerListsMap.clear();
        musicListFromData.clear();
        defaultList.clear();
        favoriteList.clear();
    }

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

    private static void initCustomerListData() {
        String query = "SELECT Music.music_bean_id, Playlist.list_name FROM Music " +
                "INNER JOIN Playlist ON Music.play_list_id = Playlist.id";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String playListName = cursor.getString(cursor.getColumnIndex("list_name"));
                if (customerListsMap.containsKey(playListName)) {
                    long musicBeanId = cursor.getLong(cursor.getColumnIndex("music_bean_id"));
                    DebugLog.debug("listName " + playListName + "; musicId " + musicBeanId);
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
     * @Title searchMusic
     * @author wm
     * @createTime 2023/2/11 15:46
     * @description 搜索设备里的音乐媒体文件
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
                musicListFromData.put(musicId, isLike);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // musicListFromData与默认列表对比，找出存在且收藏的歌，将其添加到收藏列表中
        long id = 0;
        for (int i = 0; i < defaultList.size(); i++) {
            id = defaultList.get(i).getId();
            if (musicListFromData.containsKey(id) && musicListFromData.get(id) != null) {
                defaultList.get(i).setLike(1 == musicListFromData.get(id));
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
     * @author wm
     * @createTime 2023/2/15 10:34
     * @description 初始化上次播放的信息
     */
    private static void initLastPlayInfo() {
        // 从上次播放的数据库中获取数据
        Cursor cursor = db.query(LAST_MUSIC_INFO_TABLE_NAME, null, null, null, null, null, null);
        // 存在数据才返回true
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
     * @author wm
     * @createTime 2023/2/16 17:28
     * @description 根据播放的列表和音乐的ID确定position
     */
    private static void findPositionFromList() {
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

    public static List<MediaFileBean> getDefaultList() {
        return defaultList;
    }

    public static List<MediaFileBean> getFavoriteList() {
        return favoriteList;
    }


    /**
     * @author wm
     * @createTime 2023/2/11 15:46
     * @description 将歌曲加入收藏列表
     */
    public static void addMusicToFavoriteList(MediaFileBean mediaFileBean) {
        DebugLog.debug("mediaFileBean " + mediaFileBean);
        if (favoriteList.contains(mediaFileBean)) {
            return;
        } else {
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
     * @author wm
     * @createTime 2023/2/11 15:46
     * @description 从收藏列表移除歌曲
     */
    public static void deleteMusicFromFavoriteList(MediaFileBean mediaFileBean) {
        DebugLog.debug("mediaFileBean " + mediaFileBean);
        if (favoriteList.contains(mediaFileBean)) {
            int removePosition = favoriteList.indexOf(mediaFileBean);
            DebugLog.debug("removePosition " + removePosition);
            Intent intent = new Intent(Constant.DELETE_MUSIC_ACTION);
            Bundle bundle = new Bundle();
            bundle.putInt("musicPosition", removePosition);
            bundle.putInt("musicListSource", Constant.LIST_MODE_FAVORITE);
            intent.putExtras(bundle);
            context.sendBroadcast(intent);
            favoriteList.remove(mediaFileBean);
            //从数据库中删除信息
            db.execSQL("DELETE FROM musiclistrecord WHERE musicId = ?",
                    new Long[]{mediaFileBean.getId()});
            if (defaultList.contains(mediaFileBean)) {
                //需要删除默认列表中的收藏状态,直接操作对象
                mediaFileBean.setLike(false);
                DebugLog.debug("mediaFileBean1 " + mediaFileBean);
            }
        }
    }

    public static int getLastPlayListMode() {
        return lastPlayListMode;
    }

    public static void setLastPlayListMode(int lastPlayListMode) {
        DataRefreshService.lastPlayListMode = lastPlayListMode;
        updateLastInfo();
    }

    public static int getLastPlayMode() {
        return lastPlayMode;
    }

    public static void setLastPlayMode(int lastPlayMode) {
        DataRefreshService.lastPlayMode = lastPlayMode;
        updateLastInfo();

    }

    public static int getLastPosition() {
        return lastPosition;
    }

    public static void setLastPosition(int lastPosition) {
        DataRefreshService.lastPosition = lastPosition;
    }

    public static void setLastMusicId(long lastMusicId) {
        DataRefreshService.lastMusicId = lastMusicId;
        updateLastInfo();
    }

    /**
     * @author wm
     * @createTime 2023/2/16 17:22
     * @description 更新数据库中最后播放的信息，
     */
    private static void updateLastInfo() {
        ContentValues values = new ContentValues();
        values.put("lastPlayListMode", lastPlayListMode);
        values.put("lastPlayMode", lastPlayMode);
        values.put("lastMusicId", lastMusicId);
        String selection = "infoRecord LIKE ?";
        String[] selectionArgs = {"lastMusicInfo"};
        db.update(LAST_MUSIC_INFO_TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    public static List<MusicListBean> getCustomerList() {
        List<MusicListBean> list = new ArrayList<>();
        try{
            for (Map.Entry<String,MusicListBean> entry:customerListsMap.entrySet()) {
                MusicListBean musicListBean = entry.getValue();
                list.add(musicListBean);
            }
            DebugLog.debug("lists size " + list.size());
            return list;
        } catch (Exception exception){
            DebugLog.debug("error " + exception.getMessage());
            return null;
        }
    }

    public static void createNewMusicList(String listName) {
        try{
            new Runnable(){
                @Override
                public void run() {
                    if (!customerListsMap.containsKey(listName)) {
                        DebugLog.debug("insert new list " + listName);
                        // 插入数据
                        ContentValues values = new ContentValues();
                        values.put("list_name", listName);
                        //参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
                        db.insert(CUSTOMER_LIST_TABLE_NAME, null, values);

                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            DebugLog.debug("error interrupted " );
                        }

                        String query = "SELECT id FROM Playlist WHERE list_name=" + listName;
                        Cursor cursor = db.rawQuery(query, null);
                        int listId = 0;
                        if (cursor != null && cursor.moveToFirst()) {
                            do {
                                listId = cursor.getInt(cursor.getColumnIndex("id"));
                            } while (cursor.moveToNext());
                        }
                        if (listId!=0){
                            MusicListBean musicListBean = new MusicListBean(listName);
                            musicListBean.setListId(listId);
                            // 将列表对象添加到动态列表中
                            customerListsMap.put(listName, musicListBean);
                        }
                    }
                }
            };

        } catch (Exception exception){
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    public static void deleteMusicList(String listName) {
        try{
            if (customerListsMap.containsKey(listName)) {
                DebugLog.debug("delete list " + listName);
                db.execSQL("DELETE FROM "+CUSTOMER_LIST_TABLE_NAME+" WHERE list_name = ?",
                        new String[]{listName});
                customerListsMap.get(listName).setMusicList(null);
                customerListsMap.remove(customerListsMap.get(listName));
            }
        } catch (Exception exception){
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /**
     *  往自定义列表中插入歌曲，需要满足多个条件才插入：
     *  列表存在、默认列表中包含插入的歌曲、自定义列表中未包含该歌曲
     *  @author wm
     *  @createTime 2023/8/29 22:35
     * @param listName: 列表名
     * @param musicList: 要插入的列表
     */
    public static void insertCustomerMusic(String listName, List<Long> musicList) {
        try{
            DebugLog.debug("listName "  + listName + "; musicList " + musicList);
            if (customerListsMap.containsKey(listName)) {
                int listId = customerListsMap.get(listName).getListId();
                for (long musicId : musicList) {
                    if (defaultListMap.containsKey(musicId)) {
                        if (!customerListsMap.get(listName).getMusicList().contains(defaultListMap.get(musicId))){
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
            }
        } catch (Exception exception){
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    public static void deleteCustomerMusic(String listName, List<Long> musicList){
        try{
            if (customerListsMap.containsKey(listName)) {
                for (long musicId : musicList) {
                    if (defaultListMap.containsKey(musicId)) {
                        if (customerListsMap.get(listName).getMusicList().contains(defaultListMap.get(musicId))){
                            // 列表中存在该歌曲才能删除数据
                            customerListsMap.get(listName).getMusicList().remove(defaultListMap.get(musicId));
                            db.execSQL("DELETE FROM "+CUSTOMER_MUSIC_TABLE_NAME+" WHERE music_bean_id = ?",
                                    new Long[]{musicId});
                        }
                    }
                }
            }
        } catch (Exception exception){
            DebugLog.debug("error " + exception.getMessage());
        }
    }

    /*
     * HashMap<String, MusicListBean> customerListsMap用来管理自定义的音乐列表
     * 里面的数据是从默认列表中拷贝过来的，不需要考虑是否是收藏歌曲
     * */

}