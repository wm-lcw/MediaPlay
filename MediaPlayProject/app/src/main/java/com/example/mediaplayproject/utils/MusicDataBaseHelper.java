package com.example.mediaplayproject.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 * @Classname MusicDataBaseHelper
 * @Description 数据库辅助类
 * @Version 1.0.0
 * @Date 2023/2/13 20:19
 * @Created by wm
 */
public class MusicDataBaseHelper extends SQLiteOpenHelper {

    /**
     * 创建历史播放列表的指令
     * */
    public static final String CREATE_HISTORY_LIST_TABLE = "create table historyList(" +
            "musicRecordId integer primary key autoincrement," +
            "musicId long(20))";

    /**
     * 创建上次播放信息数据表的指令
     * */
    public static final String CREATE_LAST_PLAY_INFO_TABLE = "create table lastPlayInfoRecord(" +
            "musicInfoId integer primary key autoincrement," +
            "infoRecord TEXT," +
            "lastPlayListName TEXT," +
            "lastPlayMode integer DEFAULT 0," +
            "lastMusicId long(20))";

    /**
     * 创建所有音乐列表的表，这里要先比ALL_MUSIC表先建立，因为ALL_MUSIC表中使用了AllListsTable表的id作为外键
     * */
    public static final String CREATE_ALL_MUSIC_LIST_TABLE = "CREATE TABLE IF NOT EXISTS allListsTable (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "listMode INTEGER," +
            "listName TEXT)";

    /**
     * 创建所有音乐的数据表
     * */
    public static final String CREATE_ALL_MUSIC_TABLE = "CREATE TABLE IF NOT EXISTS allMusicTable (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "musicId long(20)," +
            "musicTitle TEXT," +
            "musicArtist TEXT," +
            "playTotal INTEGER," +
            "listName TEXT," +
            "listMode INTEGER," +
            "listId INTEGER," +
            "FOREIGN KEY(listId) REFERENCES allListsTable(id)" +
            "ON DELETE CASCADE " +
            "ON UPDATE CASCADE)";

    public MusicDataBaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_LAST_PLAY_INFO_TABLE);
        db.execSQL(CREATE_HISTORY_LIST_TABLE);

        db.execSQL(CREATE_ALL_MUSIC_LIST_TABLE);
        db.execSQL(CREATE_ALL_MUSIC_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /*
    * 音乐数据表、音乐列表数据表可以只保存自定义创建的列表歌曲
    * 插入数据时不需要插入"id INTEGER PRIMARY KEY AUTOINCREMENT,"属性
    *
    *
    * 动态创建音乐列表：用动态数据结构来创建和管理这些列表。
    * 可以使用ArrayList或者LinkedList等集合类（动态数据结构）来实现动态列表。
    * List<MusicList>
    * 从音乐列表的数据表里面拿到数据，每拿到一条数据，就创建一个MusicList对象，将其放到List中
    * MusicList对象当作是一个JavaBean，里面持有一个List属性，用来存储音乐列表；
    * 其他属性：歌曲列表名、歌曲数量、列表创建时间（可根据需要拓展）-- 最好对应数据表里面的各项内容
    *
    * 原有的两个数据表（收藏列表、上次播放的列表）不用改动（上次播放的列表存储信息较少，需要优化-改成使用更轻量级的方式存储）
    *
    * 现增加的三个表用于管理自定义的列表和音乐
    * （1）音乐表：用来保存所有列表里的所有音乐，同一首歌可能会存储多条数据，因为所属列表不一样
    * （2）音乐列表的数据表：用来保存所有列表的信息，最好与MusicListBean对象对应起来，方便管理
    * （3）音乐表-列表数据表：关联音乐数据表和列表数据表（感觉没必要），上面两个表用外键约束即可
    * 外键约束：删除某个列表时，音乐表中属于该表的音乐会被一起删除，
    *
    *
    *
    *
     * */
}
