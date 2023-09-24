package com.example.mediaplayproject.utils;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

/**
 * @author wm
 * @Classname StatusBar
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/9/14 16:26
 * @Created by wm
 */
public class StatusBar {
    private Activity activity;
    public StatusBar(Activity activity){
        this.activity = activity;
    }
    public void setColor(int color){
        //将状态栏设置为传入的color
        View view = activity.getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        activity.getWindow().setStatusBarColor(activity.getResources().getColor(color));
    }

    /**
     *  隐藏状态栏
     *  @author wm
     *  @createTime 2023/9/24 21:27
     */
    public void hide(){
        activity.getWindow()
                .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     *  设置状态栏字体颜色
     *  @author wm
     *  @createTime 2023/9/24 21:27
     * @param isDarkBackground:
     */
    public void setTextColor(boolean isDarkBackground){
        View decor = activity.getWindow().getDecorView();
        if (isDarkBackground) {
            //黑暗背景字体浅色
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else {
            //高亮背景字体深色
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

}

