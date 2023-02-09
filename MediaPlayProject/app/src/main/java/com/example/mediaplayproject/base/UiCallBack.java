package com.example.mediaplayproject.base;

import android.os.Bundle;

/**
 * @author wm
 * @Classname UiCallBack
 * @Description UI回调接口
 * @Version 1.0.0
 * @Date 2023/2/9 11:08
 * @Created by wm
 */
public interface UiCallBack {

    /**
     * 初始化savedInstanceState
     *
     * @param savedInstanceState
     */
    void initBeforeView(Bundle savedInstanceState);

    /**
     * 初始化数据 相当于onCreate
     *
     * @param savedInstanceState
     */
    void initBundleData(Bundle savedInstanceState);

    /**
     * 绑定布局
     *
     * @return
     */
    int getLayoutId();
}


