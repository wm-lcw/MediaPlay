package com.example.mediaplayproject.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.example.mediaplayproject.bean.MediaFileBean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 */
public class SearchFiles {
    private static SearchFiles searchFiles;
    private static Context mContext;

    private SearchFiles (){}
    public static synchronized SearchFiles getInstance(Context context) {
        if (searchFiles == null) {
            searchFiles = new SearchFiles();
        }
        mContext = context;
        return searchFiles;
    }

    private static final Uri URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private static final String[] MUSIC_PROJECTION = new String[] {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
    };

    /** （1）"mime_type in ('audio/mpeg','audio/x-ms-wma')"：这部分条件指定了要查询的音频文件的 MIME 类型。
        在这里，它指定了两种 MIME 类型：'audio/mpeg' 和 'audio/x-ms-wma'，分别代表 MPEG 格式的音频文件和 Windows Media Audio (WMA) 格式的音频文件。
        表示查询结果将仅包含这两种格式的音频文件。可以加上'audio/flac'，可以检索到flac格式的音乐。
     （2）"bucket_display_name <> 'audio'"：这部分条件用于排除存储桶（目录）名称为 'audio' 的文件。
        通常，存储桶名称 'audio' 是用于存储系统音频文件的，例如系统通知音、铃声等。通过排除 'audio' 存储桶，你可以避免查询到这些系统音频文件。
     （3）"is_music > 0"：这部分条件用于筛选出 "is_music" 列的值大于 0 的音频文件。
        在 MediaStore.Audio.Media 表中， "is_music" 列通常用于标识音乐文件，其值大于 0 表示该文件是音乐文件。这个条件确保只有真正的音乐文件被包括在查询结果中。
     */
    private static final String SELECTION = "mime_type in ('audio/mpeg','audio/x-ms-wma','audio/flac') and bucket_display_name <> 'audio' and is_music > 0 ";
    private static final String SORT_ORDER = MediaStore.Audio.Media.DATA;

    public List<MediaFileBean> getMusicInfo() {
        List<MediaFileBean> musicInfos = new ArrayList<>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(URI, MUSIC_PROJECTION, SELECTION, null, SORT_ORDER);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MediaFileBean musicInfo = new MediaFileBean();
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                int duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));

                musicInfo.setId(id);
                if (!TextUtils.isEmpty(title)) {
                    musicInfo.setTitle(title);
                }
                if (!TextUtils.isEmpty(data)) {
                    musicInfo.setData(data);
                }
                if (!TextUtils.isEmpty(album)) {
                    musicInfo.setAlbum(album);
                }
                if (!TextUtils.isEmpty(artist)) {
                    musicInfo.setArtist(artist);
                }
                if (duration != 0) {
                    musicInfo.setDuration(duration);
                }
                if (size != 0) {
                    musicInfo.setSize(size);
                }
                if (size>=1000){
                    musicInfos.add(musicInfo);
                }
            }
            cursor.close();
        }
        DebugLog.debug("local musicList size " + musicInfos.size());
        return musicInfos;
    }


}
