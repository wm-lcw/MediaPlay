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

    public static final String LIST_MODE_DEFAULT_NAME = "默认列表";
    public static final String LIST_MODE_FAVORITE_NAME = "收藏列表";
    public static final String LIST_MODE_CUSTOMER_NAME = "自定义";
    public static final String LIST_MODE_HISTORY_NAME = "最近播放";

    public static final int LIST_SHOW_MODE_CURRENT = 1;
    public static final int LIST_SHOW_MODE_FAVORITE = 2;
    public static final int LIST_SHOW_MODE_HISTORY = 3;

    public static final int LIST_SAVE_LIST_MODE_DEFAULT = 1;
    public static final int LIST_SAVE_LIST_MODE_FAVORITE = 2;
    public static final int LIST_SAVE_LIST_MODE_CUSTOMER = 3;


    public final static int RESULT_IS_CURRENT_POSITION = 0;
    public final static int RESULT_BEFORE_CURRENT_POSITION = 1;
    public final static int RESULT_AFTER_CURRENT_POSITION = 2;

    public static final int HANDLER_MESSAGE_REFRESH_VOLUME = 0;
    public static final int HANDLER_MESSAGE_REFRESH_PLAY_STATE = 1;
    public static final int HANDLER_MESSAGE_REFRESH_LIST_STATE = 2;
    public static final int HANDLER_MESSAGE_REFRESH_FROM_PLAY_HELPER = 3;
    public static final int HANDLER_MESSAGE_SHOW_LIST_FRAGMENT = 4;
    public static final int HANDLER_MESSAGE_DELAY_INIT_MAIN_ACTIVITY = 5;
    public static final int HANDLER_MESSAGE_DELAY_INIT_FRAGMENT = 6;
    public static final int HANDLER_MESSAGE_DELAY_OPEN_KEYBOARD = 7;
    public static final int HANDLER_MESSAGE_REFRESH_STATISTICS_DATA = 8;
    public static final int HANDLER_MESSAGE_DELAY_TIMING_OFF = 9;

    public final static int CUSTOMER_LIST_OPERATOR_CREATE = 0;
    public final static int CUSTOMER_LIST_OPERATOR_DELETE = 1;
    public final static int CUSTOMER_MUSIC_OPERATOR_INSERT = 3;
    public final static int CUSTOMER_MUSIC_OPERATOR_DELETE = 4;

    public static final String CHANGE_MUSIC_ACTION = "com.example.media.play.change.music.action";
    public static final String DELETE_MUSIC_ACTION = "com.example.media.play.delete.music.action";
    public static final String OPERATE_CUSTOMER_MUSIC_LIST_ACTION = "com.example.media.play.operate.customer.music.list.action";
    public static final String OPERATE_MUSIC_ACTION = "com.example.media.play.operate.music.action";
    public static final String STOP_PLAY_CUSTOMER_MUSIC_ACTION = "com.example.media.play.stop.play.customer.music.action";
    public static final String REFRESH_SEARCH_RESULT_ACTION = "com.example.media.play.refresh.search.result.action";
    public static final String CHANGE_FRAGMENT_ACTION = "com.example.media.play.change.fragment.action";
    public static final String RETURN_MAIN_VIEW_ACTION = "com.example.media.play.return.main.view.action";
    public static final String CHANGE_TIMING_OFF_TIME_ACTION = "com.example.media.play.change.timing.off.time.action";

    public static final int MAX_LENGTH_OF_LIST_NAME = 10;
    public static final int HISTORY_LIST_MAX_SIZE = 50;

    public static final int SEARCH_ALL_MUSIC_FLAG = 0;
    public static final int SEARCH_LIST_MUSIC_FLAG = 1;

    public static final String STATISTICS_FRAGMENT_ACTION_FLAG = "StatisticsFragment";
    public static final String MUSIC_PLAY_FRAGMENT_ACTION_FLAG = "MusicPlayFragment";
    public static final String TOOLS_FRAGMENT_ACTION_FLAG = "ToolsFragment";
    public static final String CHANGE_LANGUAGE_FRAGMENT_ACTION_FLAG = "ChangeLanguageFragment";
    public static final String TIMING_OFF_FRAGMENT_ACTION_FLAG = "TimingOffFragment";
    public static final String WOODEN_FISH_FRAGMENT_ACTION_FLAG = "WoodenFishFragment";
    public static final String ABOUT_FRAGMENT_ACTION_FLAG = "AboutFragment";
    public static final String SETTINGS_FRAGMENT_ACTION_FLAG = "SettingsFragment";

    public static final String CURRENT_LANGUAGE = "language";
    public static final String CURRENT_COUNTRY = "country";
    public static final String CURRENT_USE_LANGUAGE = "currentLanguage";
    public static final String SWITCH_LANGUAGE = "switchLanguage";
    public static final String SHORTCUT_TOOLS_LIST = "shortcutToolsList";

    public static final String TIMING_OFF_TYPE = "timingOffType";
    public static final String TIMING_OFF_TIME = "timingOffTime";

    public static final String WOODEN_FISH_COUNT = "woodenFishCount";

    public static final int MAX_SHORTCUT_TOOLS_NUM = 3;

    public static final String STATISTICS_SUB = "statisticsSub";
    public static final String STATISTICS_TEXT = "statisticsText";
    public static final String STATISTICS_TIME = "statisticsTime";

    public static final String LOG_SWITCH = "logSwitch";
    public static final String LOG_LEVEL = "logLevel";
    public static final String LOG_WRITE = "logWrite";

    public static final String LOCK_SCREEN_SWITCH = "lockScreenSwitch";

    public static final String REFRESH_PLAY_STATE_ACTION = "refresh.play.state.action";

    public static final int REQUEST_CODE_OPEN_DIRECTORY = 123;
}
