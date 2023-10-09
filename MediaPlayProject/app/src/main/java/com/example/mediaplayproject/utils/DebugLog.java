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
     * ·�� "/storage/emulated/0/mediaPlayLog"
     * @param msg ��Ҫ��ӡ������
     */
    public static void writerLog(int logLevel, String msg) {
        if (LEVEL_FILE == logLevel) {
            //���浽���ļ�·��
            final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            FileWriter fileWriter;
            BufferedWriter bufferedWriter = null;

            try {
                //�����ļ���
                File dir = new File(filePath, "mediaPlayLog");
                if (!dir.exists()) {
                    dir.mkdir();
                }
                //�����ļ�
                File file = new File(dir, "mediaPlayLog.txt");
                if (!file.exists()) {
                    file.createNewFile();
                }
                //д����־�ļ�
                fileWriter = new FileWriter(file, true);
                bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(  getCurrentTime() + "---" + msg  + "\n");
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
     *  ��ȡ��ǰʱ��
     *  @author wm
     *  @createTime 2023/10/9 20:27
     * @return : java.lang.String
     */
    private static String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(calendar.getTime());
    }
}
