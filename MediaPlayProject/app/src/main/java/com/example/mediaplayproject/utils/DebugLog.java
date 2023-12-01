package com.example.mediaplayproject.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @description 日志工具类
 * @author wm
 *
 * 在Application 初始化Log的开关，可以在软件内设置Log的开关，重启后生效
 * 全局都可以使用这个打印工具类，普通日志使用inform，调试信息使用debug，错误信息使用error
 */
public class DebugLog {
    private static boolean LOCAL_DBG_SWITCH = true;
    private static String TAG = "MediaProjectLog";
    /**
     *  写入文件的log等级
     * */
    private static int LEVEL_FILE = 1;
    /**
     * 当前设定的打印等级
     * */
    private static int LOG_LEVEL = 1;
    private static final int LEVEL_VERBOSE = 1;
    private static final int LEVEL_DEBUG = 2;
    private static final int LEVEL_INFO = 3;
    private static final int LEVEL_WARNING = 4;
    private static final int LEVEL_ERROR = 5;

    /**
     *  初始化打印的信息
     *  @author wm
     *  @createTime 2023/12/1 17:44
     * @param context: 上下文
     * @param isOpenLog:  是否开启打印
     * @param logLevel: 打印等级, 当前设置等级小于设定的等级才能打印
     */
    public static void init(Context context, boolean isOpenLog,int logLevel){
        TAG = getAppName(context);
        LOCAL_DBG_SWITCH = isOpenLog;
        LOG_LEVEL = logLevel;
        Log.d(TAG, "init: isOpenLog " + isOpenLog + "; logLevel " + logLevel);
    }

    /**
     *  调试日志
     *  @author wm
     *  @createTime 2023/12/1 16:59
     * @param log:
     */
    public static void debug(String log) {
        if (LOCAL_DBG_SWITCH && LOG_LEVEL <= LEVEL_DEBUG) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];

            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.d(TAG, strMsg);
            writerLog(LEVEL_FILE, strMsg);
        }
    }

    /**
     *  普通日志打印，如：不频繁的操作日志，开始、结束、重要过程等
     *  @author wm
     *  @createTime 2023/12/1 16:59
     * @param log:
     */
    public static void info(String log) {
        if (LOCAL_DBG_SWITCH && LOG_LEVEL <= LEVEL_INFO) {
            // currentStack不要抽离成单独的方法，否则会导致只打印原本的堆栈信息
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];
            // 添加类名、方法名和对应行数
            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.i(TAG, strMsg);
            writerLog(LEVEL_FILE, strMsg);
        }
    }



    /**
     *  异常日志打印
     *  @author wm
     *  @createTime 2023/12/1 17:00
     * @param log:
     */
    public static void error(String log) {
        if (LOCAL_DBG_SWITCH && LOG_LEVEL <= LEVEL_ERROR) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];

            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.e(TAG, strMsg);
            writerLog(LEVEL_FILE, strMsg);
        }
    }

    /**
     * 将打印信息写入Log文件中
     * 路径 "/storage/emulated/0/mediaPlayLog"
     * @param msg 需要打印的内容
     */
    public static void writerLog(int logLevel, String msg) {
        if (LEVEL_FILE == logLevel) {
            //保存到的文件路径
            final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            FileWriter fileWriter;
            BufferedWriter bufferedWriter = null;

            try {
                //创建文件夹
                File dir = new File(filePath , "MediaPlay");
                if (!dir.exists()) {
                    dir.mkdir();
                }
                //创建文件夹
                File dir2 = new File(dir, "Log");
                if (!dir2.exists()) {
                    dir2.mkdir();
                }
                //创建文件
                File file = new File(dir2, "mediaPlayLog.txt");
                if (!file.exists()) {
                    file.createNewFile();
                }
                //写入日志文件
                fileWriter = new FileWriter(file, true);
                bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(  ToolsUtils.getCurrentTime() + "---" + msg  + "\n");
                bufferedWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Log.d("mediaPlayLog", msg+"");
        }
    }

    /**
     *  根据报名获取app名字
     *  @author wm
     *  @createTime 2023/12/1 17:51
     * @param context:
     * @return : java.lang.String
     */
    private static String getAppName(Context context){
        PackageManager packageManager = context.getPackageManager();
        String pkgName = context.getPackageName();
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(pkgName, 0);
            return info.loadLabel(packageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "UnKnow" + TAG;
    }
}
