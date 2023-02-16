package com.example.mediaplayproject.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

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
     * 创建收藏列表数据表的指令
     * */
    public static final String CREATE_MUSIC_LIST_TABLE= "create table musiclistrecord(musicRecordId integer primary key autoincrement,musicId long(20),isLike integer)";

    /**
     * 创建上次播放信息数据表的指令
     * */
    public static final String CREATE_LAST_PLAY_INFO_TABLE= "create table lastplayinforecord(musicInfoId integer primary key autoincrement,infoRecord TEXT,lastPlayListMode integer DEFAULT 0,lastPlayMode integer DEFAULT 0,lastMusicId long(20))";

    public MusicDataBaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MUSIC_LIST_TABLE);
        db.execSQL(CREATE_LAST_PLAY_INFO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
