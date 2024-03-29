package com.example.mediaplayproject.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.ToolsBean;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
            case 5:
                fragmentName = Constant.SETTINGS_FRAGMENT_ACTION_FLAG;
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

    /**
     *  所有小工具的图标，打开对应的Fragment需要用到下标，所以下标要跟string.xml中定义的tools_item_title一一对应
     *  后续增删小工具、调换小工具的位置时，都要核对各个下标以及Fragment的启动id。
     *  @author wm
     *  @createTime 2023/12/18 16:22
     */
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


    /**
     * 获取当前本地apk的版本
     * @param mContext: 上下文
     */
    public int getVersionCode(Context mContext) {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = mContext.getPackageManager().
                    getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 获取版本号名称
     *
     * @param context 上下文
     */
    public String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

    /**
     *  获取当前时间，格式 yyyy-MM-dd HH:mm:ss
     *  @author wm
     *  @createTime 2023/10/9 20:27
     * @return : java.lang.String
     */
    public static String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(calendar.getTime());
    }

    /**
     *  获取当前时间，格式为 yyyyMMddHHmmss，不带空格，用于创建文件名
     *  @author wm
     *  @createTime 2023/12/1 16:45
     * @return : java.lang.String
     */
    public static String getCurrentTime2() {
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(calendar.getTime());
    }

    /**
     *  获取歌曲专辑图片
     *  @author wm
     *  @createTime 2023/9/24 19:40
     *  @param context: 上下文
     *  @param dataPath: 音乐资源的路径
     *  @param isRemoteView: 是否用于通知栏图标， true:通知栏和小部件的图标； false:播放页的专辑图片
     *                     通知栏的图标不用设置那么清晰，避免出现内存占用过高，最终触发异常
     * @return : android.graphics.Bitmap, 可以为空
     */
    public static Bitmap getAlbumPicture(Context context, String dataPath, boolean isRemoteView) {
        try {
            Bitmap albumPicture;
            int multiple = 160;
            if (!isRemoteView){
                // 播放页的专辑图片，更改清晰度
                multiple = 640;
            }
            android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(dataPath);
            byte[] data = mmr.getEmbeddedPicture();
            if (data != null) {
                // 获取bitmap对象
                albumPicture = BitmapFactory.decodeByteArray(data, 0, data.length);
                // 获取宽高
                int width = albumPicture.getWidth();
                int height = albumPicture.getHeight();
                // 创建操作图片用的Matrix对象
                Matrix matrix = new Matrix();
                // 计算缩放比例,数值越大，图片越清晰
                float sx = ((float) multiple / width);
                float sy = ((float) multiple / height);
                // 设置缩放比例
                matrix.postScale(sx, sy);
                // 建立新的bitmap，其内容是对原bitmap的缩放后的图
                albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);

                /// 将专辑转为圆形的bitmap--start
                // 创建一个正方形的Bitmap
                Bitmap squareBitmap = Bitmap.createBitmap(albumPicture.getWidth(), albumPicture.getHeight(), Bitmap.Config.ARGB_8888);
                // 创建画布，并将squareBitmap绘制到画布上
                Canvas canvas = new Canvas(squareBitmap);
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                canvas.drawBitmap(albumPicture, 0, 0, paint);
                // 获取图片的宽高中最小的值作为圆形半径
                int radius = Math.min(albumPicture.getWidth(), albumPicture.getHeight()) / 2;
                // 创建一个新的Bitmap，作为圆形图片
                Bitmap circleBitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
                canvas.setBitmap(circleBitmap);
                // 创建一个Path，并添加一个圆形的路径
                Path path = new Path();
                path.addCircle(radius, radius, radius, Path.Direction.CW);
                // 裁剪画布为圆形路径
                canvas.clipPath(path);
                paint.reset();
                paint.setAntiAlias(true);
                // 将原图片绘制到圆形画布上
                canvas.drawBitmap(squareBitmap, new Rect(0, 0, squareBitmap.getWidth(), squareBitmap.getHeight()), new Rect(0, 0, radius * 2, radius * 2), paint);
                // 释放bitmap资源
                albumPicture.recycle();
                squareBitmap.recycle();
                // 更新albumPicture为圆形图片
                albumPicture = circleBitmap;
                /// 将专辑转为圆形的bitmap--end
            } else {
                albumPicture = BitmapFactory.decodeResource(context.getResources(), R.drawable.music);
                //music是从歌曲文件读取不出来专辑图片时用来代替的默认专辑图片
                int width = albumPicture.getWidth();
                int height = albumPicture.getHeight();
                // 创建操作图片用的Matrix对象
                Matrix matrix = new Matrix();
                // 计算缩放比例
                float sx = ((float) multiple / width);
                float sy = ((float) multiple / height);
                // 设置缩放比例
                matrix.postScale(sx, sy);
                // 建立新的bitmap，其内容是对原bitmap的缩放后的图
                albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
            }
            return albumPicture;
        } catch (Exception exception){
            DebugLog.error("error " + exception);
            return null;
        }
    }
}
