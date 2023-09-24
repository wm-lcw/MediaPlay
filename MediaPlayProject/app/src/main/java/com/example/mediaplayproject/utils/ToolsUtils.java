package com.example.mediaplayproject.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Locale;


/**
 * @author wm
 * @Description TODO:工具类
 * @Date 2023/9/11 17:41
 */
public class ToolsUtils {

    private static ToolsUtils instance;
    public static ToolsUtils getInstance() {
        if (instance == null) {
            instance = new ToolsUtils();
        }
        return instance;
    }

    public ToolsUtils() {
    }

    /**
     *  关闭键盘，因有多处地方调用，所以将其抽象出来作为工具类里面的方法
     *  @author wm
     *  @createTime 2023/9/11 17:40
     */
    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    /**
     *  开启键盘
     *  @author wm
     *  @createTime 2023/9/21 17:52
     * @param view:
     */
    public void showKeyBoard(View view){
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public final void changeLanguage(Context context, String language, String country) {
        if (context == null || TextUtils.isEmpty(language)) {
            return;
        }
        DebugLog.debug(language + " " + country);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        config.locale = new Locale(language, country);
        resources.updateConfiguration(config, displayMetrics);
    }

    /**
     * @author wm
     * @createTime 2023/2/3 18:40
     * @description 格式化获取到的时间
     */
    public String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            return (time / 1000 / 60) + ":0" + time / 1000 % 60;
        } else {
            return (time / 1000 / 60) + ":" + time / 1000 % 60;
        }
    }
}
