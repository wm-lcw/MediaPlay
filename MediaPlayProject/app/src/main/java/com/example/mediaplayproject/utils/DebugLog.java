package com.example.mediaplayproject.utils;

/**
 * @author wm
 */
public class DebugLog {
    private static final boolean LOCAL_DBG_SWITCH = true;
    private static final String TAG = "MediaProjectLog";

    public static void info(String log) {
        if (LOCAL_DBG_SWITCH) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];

            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.i(TAG, strMsg);
        }
    }

    public static void debug(String log) {
        if (LOCAL_DBG_SWITCH) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];

            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.d(TAG, strMsg);
        }
    }

    public static void error(String log) {
        if (LOCAL_DBG_SWITCH) {
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            StackTraceElement currentStack = stacks[1];

            String strMsg = currentStack.getFileName() + "(" + currentStack.getLineNumber() + ")::"
                    + currentStack.getMethodName() + " - " + log;
            android.util.Log.e(TAG, strMsg);
        }
    }
}
