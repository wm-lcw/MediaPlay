package com.example.mediaplayproject.base;

import android.app.Activity;

import com.example.mediaplayproject.utils.DebugLog;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 * @Classname ActivityManager
 * @Description 管理所有的Activity
 * @Version 1.0.0
 * @Date 2023/2/9 10:55
 * @Created by wm
 */
public class ActivityManager {
    /**
     * 保存所有创建的Activity
     */
    private List<Activity> allActivities = new ArrayList<>();

    /**
     * 添加Activity到管理器
     *
     * @param activity activity
     */
    public void addActivity(Activity activity) {
        if (activity != null) {
            DebugLog.debug("" + activity);
            allActivities.add(activity);
        }
    }


    /**
     * 从管理器移除Activity
     *
     * @param activity activity
     */
    public void removeActivity(Activity activity) {
        if (activity != null) {
            allActivities.remove(activity);
        }
    }

    /**
     * 关闭所有Activity
     */
    public void finishAll() {
        for (Activity activity : allActivities) {
            activity.finish();
        }

    }

    /**
     * 获取栈顶的Activity
     *
     * @return
     */
    public Activity getTaskTop() {
        return allActivities.get(allActivities.size() - 1);
    }
}


