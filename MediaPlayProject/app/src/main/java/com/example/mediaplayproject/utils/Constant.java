package com.example.mediaplayproject.utils;

/**
 * @author wm
 * @Description 常量类
 * @Date 2023/8/4 22:15
 */
public class Constant {
    public static final int PLAY_MODE_LOOP = 0;
    public static final int PLAY_MODE_SHUFFLE = 1;
    public static final int PLAY_MODE_SINGLE = 2;

    public static final int LIST_MODE_DEFAULT = 0;
    public static final int LIST_MODE_FAVORITE = 1;

    public final static int RESULT_IS_CURRENT_POSITION = 0;
    public final static int RESULT_BEFORE_CURRENT_POSITION = 1;
    public final static int RESULT_AFTER_CURRENT_POSITION = 2;

    public static final int HANDLER_MESSAGE_REFRESH_VOLUME = 0;
    public static final int HANDLER_MESSAGE_REFRESH_PLAY_ICON = 1;
    public static final int HANDLER_MESSAGE_REFRESH_POSITION = 2;
    public static final int HANDLER_MESSAGE_REFRESH_FROM_PLAY_HELPER = 3;
    public static final int HANDLER_MESSAGE_FROM_LIST_FRAGMENT = 4;

    public static final String DELETE_MUSIC_ACTION = "com.example.media.play.delete.music.action";
}
