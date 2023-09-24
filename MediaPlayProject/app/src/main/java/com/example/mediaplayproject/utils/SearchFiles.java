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

    /** ��1��"mime_type in ('audio/mpeg','audio/x-ms-wma')"���ⲿ������ָ����Ҫ��ѯ����Ƶ�ļ��� MIME ���͡�
        �������ָ�������� MIME ���ͣ�'audio/mpeg' �� 'audio/x-ms-wma'���ֱ���� MPEG ��ʽ����Ƶ�ļ��� Windows Media Audio (WMA) ��ʽ����Ƶ�ļ���
        ��ʾ��ѯ����������������ָ�ʽ����Ƶ�ļ������Լ���'audio/flac'�����Լ�����flac��ʽ�����֡�
     ��2��"bucket_display_name <> 'audio'"���ⲿ�����������ų��洢Ͱ��Ŀ¼������Ϊ 'audio' ���ļ���
        ͨ�����洢Ͱ���� 'audio' �����ڴ洢ϵͳ��Ƶ�ļ��ģ�����ϵͳ֪ͨ���������ȡ�ͨ���ų� 'audio' �洢Ͱ������Ա����ѯ����Щϵͳ��Ƶ�ļ���
     ��3��"is_music > 0"���ⲿ����������ɸѡ�� "is_music" �е�ֵ���� 0 ����Ƶ�ļ���
        �� MediaStore.Audio.Media ���У� "is_music" ��ͨ�����ڱ�ʶ�����ļ�����ֵ���� 0 ��ʾ���ļ��������ļ����������ȷ��ֻ�������������ļ��������ڲ�ѯ����С�
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
