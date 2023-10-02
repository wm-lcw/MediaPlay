package com.example.mediaplayproject.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.ToolsBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


/**
 * @author wm
 * @Description TODO:工具类
 * @Date 2023/9/11 17:41
 */
public class ToolsUtils {

    private static ToolsUtils instance;
    private SoundPool.Builder builder;
    private SoundPool soundpool;
    private int soundId;

    public static ToolsUtils getInstance() {
        if (instance == null) {
            instance = new ToolsUtils();
        }
        return instance;
    }

    public ToolsUtils() {
    }

    /**
     * 关闭键盘，因有多处地方调用，所以将其抽象出来作为工具类里面的方法
     *
     * @author wm
     * @createTime 2023/9/11 17:40
     */
    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }

    /**
     * 开启键盘
     *
     * @param view:
     * @author wm
     * @createTime 2023/9/21 17:52
     */
    public void showKeyBoard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * 切换语言
     *
     * @param context:  上下文
     * @param language: 语言
     * @param country:  国家
     * @author wm
     * @createTime 2023/9/26 16:08
     */
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

    /**
     * 根据传入小工具的ID，启动对应的小工具Fragment（若小工具的展示形式不是Fragment，则不能以这种方式启动）
     * 这里的ID对应的是ToolsFragment.TOOLS_ITEM_ICON_LIST的下标，创建小工具Bean时默认使用下标作为Id
     *
     * @param context: 上下文
     * @param toolsId: 小工具的ItemID
     * @author wm
     * @createTime 2023/9/26 16:12
     */
    public void startToolsFragmentById(Context context, int toolsId) {
        String fragmentName = "";
        switch (toolsId) {
            case -1:
                fragmentName = Constant.TOOLS_FRAGMENT_ACTION_FLAG;
                break;
            case 0:
                fragmentName = Constant.STATISTICS_FRAGMENT_ACTION_FLAG;
                break;
            case 1:
                fragmentName = Constant.TIMING_OFF_FRAGMENT_ACTION_FLAG;
                break;
            case 2:
                fragmentName = Constant.CHANGE_LANGUAGE_FRAGMENT_ACTION_FLAG;
                break;
            case 3:
                fragmentName = Constant.WOODEN_FISH_FRAGMENT_ACTION_FLAG;
                break;
            default:
                break;

        }
        if ("".equals(fragmentName)) {
            return;
        }
        changeFragment(context, fragmentName);
    }

    /**
     *  切换Fragment的广播处理
     *  @author wm
     *  @createTime 2023/10/2 22:46
     * @param context: 上下文
     * @param fragmentName: 要切换的fragment标识
     */
    public void changeFragment(Context context, String fragmentName){
        Intent intent = new Intent(Constant.CHANGE_FRAGMENT_ACTION);
        Bundle bundle = new Bundle();
        bundle.putString("fragment", fragmentName);
        intent.putExtras(bundle);
        context.sendBroadcast(intent);
    }

    private static final int[] TOOLS_ITEM_ICON_LIST = {
            R.mipmap.ic_tools_history_record_blue, R.mipmap.ic_tools_timing_blue, R.mipmap.ic_tools_change_language_blue,
            R.mipmap.ic_tools_wooden_blue, R.mipmap.ic_tools_my_blue, R.mipmap.ic_tools_settings_blue,
    };

    /**
     * 获取所有工具列表
     *
     * @param mContext:
     * @return : java.util.List<com.example.mediaplayproject.bean.ToolsBean>
     * @author wm
     * @createTime 2023/9/26 17:24
     */
    public List<ToolsBean> getAllToolsList(Context mContext) {
        List<ToolsBean> allToolsBeanList = new ArrayList<>();
        ArrayList<String> itemTitleList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.tools_item_title)));
        // 这里应该做数量判断，后续加上
        for (int i = 0; i < itemTitleList.size(); i++) {
            ToolsBean toolsBean = new ToolsBean(i, itemTitleList.get(i), TOOLS_ITEM_ICON_LIST[i]);
            allToolsBeanList.add(toolsBean);
        }
        return allToolsBeanList;
    }


    /**
     * 获取快捷工具列表
     * 这shortcutToolsBeanList的对象必须从allToolsBeanList中获取才行,否则两个列表里的对象并非同一对象，没办法删除
     *
     * @param mContext:
     * @return : java.util.List<com.example.mediaplayproject.bean.ToolsBean>
     * @author wm
     * @createTime 2023/9/26 17:45
     */
    public List<ToolsBean> getShortcutToolsList(Context mContext, List<ToolsBean> allToolsList) {
        List<ToolsBean> shortcutToolsBeanList = new ArrayList<>();
        List<Integer> saveList = SharedPreferencesUtil.getListData(Constant.SHORTCUT_TOOLS_LIST, Integer.class);
        for (int i = 0; i < saveList.size(); i++) {
            shortcutToolsBeanList.add(allToolsList.get(saveList.get(i)));
        }
        return shortcutToolsBeanList;
    }

    /**
     * 返回主页的操作
     *
     * @param mContext:
     * @author wm
     * @createTime 2023/9/27 23:22
     */
    public void backToMainViewFragment(Context mContext) {
        Intent intent = new Intent(Constant.RETURN_MAIN_VIEW_ACTION);
        mContext.sendBroadcast(intent);
    }


    /**
     *  播放音频的方法，这里用于电子木鱼的点击音效
     *  @author wm
     *  @createTime 2023/9/30 21:00
     * @param context:上下文
     */
    public void audioPlay(Context context) {
        builder = new SoundPool.Builder();
        //AudioAttributes是一个封装音频各种属性的方法
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        //设置音频流的合适的属性
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_SYSTEM);
        soundpool = builder.build();
        soundId = soundpool.load(context, R.raw.muyu, 1);
        //是否加载完成的监听
        soundpool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            //加载完毕后再播放
            soundpool.play(soundId, 1f, 1f, 0, 0, 1);
        });
    }

}
