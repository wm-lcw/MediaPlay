package com.example.mediaplayproject.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.Locale;

/**
 * @author wm
 * @Description 多语言工具类
 */
public class MultiLanguageUtil {
    private static final String TAG = "MultiLanguageUtil";

    /**
     *  辅助初始化，在BaseApplication的attachBaseContext中调用
     *  实测不调用该方法也能实现切换语言功能
     *  @author wm
     *  @createTime 2023/10/9 16:10
     * @param context:
     * @return : android.content.Context
     */
    public static Context attachBaseContext(Context context) {
        Log.d(TAG, "attachBaseContext: ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return createConfigurationResources(context);
        } else {
            setConfiguration(context);
            return context;
        }
    }

    /**
     *  初始化local语言信息
     *  @author wm
     *  @createTime 2023/10/9 14:03
     * @param context: 上下文
     * @return : java.util.Locale
     */
    public static Locale initLocal(Context context){
        Locale appLocale = getAppLocale(context);
        Locale locale;
        String spLanguage = (String) SharedPreferencesUtil.getData(Constant.CURRENT_LANGUAGE, "");
        String spCountry = (String) SharedPreferencesUtil.getData(Constant.CURRENT_COUNTRY, "");
        if (!TextUtils.isEmpty(spLanguage) && !TextUtils.isEmpty(spCountry)) {
            if (isSameLocal(appLocale, spLanguage, spCountry)) {
                // 设置了app语言信息，且与系统语言一致，使用系统语言
                locale = appLocale;
            } else {
                // 设置了app语言信息，且与系统语言不一致，则使用设定的语言
                locale = new Locale(spLanguage, spCountry);
            }
        } else {
            // SharedPreferencesUtil中的信息为空，说明设置了跟随系统语言
            locale = appLocale;
        }
        Log.d(TAG, "after initLocal: " + locale.getLanguage() + "/" + locale.getCountry());
        return locale;
    }

    /**
     * 设置语言（安卓7之前）
     * @param context: 上下文
     */
    public static void setConfiguration(Context context) {
        Log.d(TAG, "setConfiguration: ");
        Locale locale = initLocal(context);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        // 语言更换生效的代码!
        resources.updateConfiguration(configuration, dm);
    }

    /**
     *  设置语言（安卓7之后）
     *  @author wm
     *  @createTime 2023/10/8 17:00
     * @param context: 上下文
     * @return : android.content.Context
     */
    @TargetApi(Build.VERSION_CODES.N)
    private static Context createConfigurationResources(Context context) {
        Log.d(TAG, "createConfigurationResources: ");
        Locale locale = initLocal(context);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }

    /**
     * 更改应用语言
     * @param context : 上下文
     * @param locale :     语言地区
     * @param persistence : 是否持久化；
     *        如果是手动设置了语言，需要将语言信息保存；
     *        如果是跟随系统语言，在初始化时调用该方法，不需要保存信息（每次启动app时识别和使用系统的语言）；
     */
    public static void changeAppLanguage(Context context, Locale locale, boolean persistence) {
        Log.d(TAG, "changeAppLanguage: " + locale.getLanguage());
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLocales(new LocaleList(locale));
            context.createConfigurationContext(configuration);
        } else {
            configuration.setLocale(locale);
        }
        resources.updateConfiguration(configuration, metrics);

        if (persistence) {
            saveLanguageSetting(context, locale);
        }
    }

    /**
     *  保存多语言信息到sp中
     *  @author wm
     *  @createTime 2023/10/8 16:13
     * @param context:
     * @param locale:
     */
    public static void saveLanguageSetting(Context context, Locale locale) {
        Log.d(TAG, "saveLanguageSetting: " + locale.getLanguage());
        try {
            boolean result = SharedPreferencesUtil.putData(Constant.CURRENT_LANGUAGE, locale.getLanguage());
            boolean result2 = SharedPreferencesUtil.putData(Constant.CURRENT_COUNTRY, locale.getCountry());
            Log.d(TAG, "saveLanguageSetting: result " + result + "/" + result2);
        } catch (Exception exception){
            Log.d(TAG, "saveLanguageSetting: error "+ exception.getMessage());
        }

    }

    /**
     *  获取本地应用的实际多语言信息（系统语言）
     *  @author wm
     *  @createTime 2023/10/8 16:12
     * @param context: 上下文
     * @return : java.util.Locale：语言的封装类
     */
    public static Locale getAppLocale(Context context) {
        // 获取应用语言
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = configuration.getLocales().get(0);
        } else {
            locale = configuration.locale;
        }
        return locale;
    }

    /**
     *  判断sp中和系统的多语言信息是否相同
     *  @author wm
     *  @createTime 2023/10/8 16:12
     *  @param context: 上下文
     *  @return : boolean true-不相同； false-相同；
     */
    public static boolean isSameWithSetting(Context context) {
        Locale locale = getAppLocale(context);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String spLanguage = (String) SharedPreferencesUtil.getData(Constant.CURRENT_LANGUAGE,"");
        String spCountry = (String) SharedPreferencesUtil.getData(Constant.CURRENT_COUNTRY,"");
        return !language.equals(spLanguage) || !country.equals(spCountry);
    }

    /**
     *  判断传进来的两个语言信息是否一致
     *  @author wm
     *  @createTime 2023/10/8 16:51
     * @param appLocale: 第一个local语言对象
     * @param language: 第二个语言对象的语言
     * @param country: 第二个语言对象的地区
     * @return : boolean: 返回值-true一致； false不一致
     */
    public static boolean isSameLocal(Locale appLocale, String language, String country) {
        String appLanguage = appLocale.getLanguage();
        String appCountry = appLocale.getCountry();
        return appLanguage.equals(language) && appCountry.equals(country);
    }
}
