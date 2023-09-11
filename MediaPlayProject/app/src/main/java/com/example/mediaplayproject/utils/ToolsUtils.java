package com.example.mediaplayproject.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


/**
 * @author wm
 * @Classname ToolsUtils
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/9/11 17:41
 * @Created by wm
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
}
