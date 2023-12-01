package com.example.mediaplayproject.utils;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author wm
 */
public class DebugLog {
    private static final boolean LOCAL_DBG_SWITCH = true;
    private static final String TAG = "MediaProjectLog";
    private static final int LEVEL_FILE = 0x2;

    public static void info(String log) {
        if (LOCAL_DBG_SWITCH) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];

            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.i(TAG, strMsg);
            writerLog(LEVEL_FILE, strMsg);
        }
    }

    public static void debug(String log) {
        if (LOCAL_DBG_SWITCH) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];

            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.d(TAG, strMsg);
            writerLog(LEVEL_FILE, strMsg);
        }
    }

    public static void error(String log) {
        if (LOCAL_DBG_SWITCH) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];

            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.e(TAG, strMsg);
            writerLog(LEVEL_FILE, strMsg);
        }
    }

    /**
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
}
