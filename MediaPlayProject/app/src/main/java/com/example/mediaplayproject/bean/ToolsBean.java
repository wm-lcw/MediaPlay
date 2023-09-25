package com.example.mediaplayproject.bean;

import android.graphics.drawable.Drawable;

/**
 * @author wm
 * @Classname ToolsBean
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/9/25 20:02
 * @Created by wm
 */
public class ToolsBean {
    private int toolsId;
    private String toolsName;
    private int toolsIconId;

    public ToolsBean(int toolsId, String toolsName, int toolsIconId) {
        this.toolsId = toolsId;
        this.toolsName = toolsName;
        this.toolsIconId = toolsIconId;
    }

    public int getToolsId() {
        return toolsId;
    }

    public void setToolsId(int toolsId) {
        this.toolsId = toolsId;
    }

    public String getToolsName() {
        return toolsName;
    }

    public void setToolsName(String toolsName) {
        this.toolsName = toolsName;
    }

    public int getToolsIconId() {
        return toolsIconId;
    }

    public void setToolsIconId(int toolsIconId) {
        this.toolsIconId = toolsIconId;
    }
}
