package com.example.mediaplayproject.view;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TimePicker;

/**
 * @author wm
 * @Classname CustomTimePickerDialog
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/9/28 15:17
 * @Created by wm
 */
public class CustomTimePickerDialog extends TimePickerDialog {

    private boolean isTimeSet = false;

    public void setTimeSet(boolean timeSet) {
        isTimeSet = timeSet;
    }

    public CustomTimePickerDialog(Context context, OnTimeSetListener listener, int hourOfDay, int minute, boolean is24HourView) {
        super(context, listener, hourOfDay, minute, is24HourView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 在这里设置对话框的标题、按钮文本等
//        setTitle("自定义标题");
//        setButton(BUTTON_POSITIVE, "确定", this);
//        setButton(BUTTON_NEGATIVE, "取消", this);
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        super.onTimeChanged(view, hourOfDay, minute);
        isTimeSet = true;
    }

    private OnCancelListener onCancelListener;

    @Override
    protected void onStop() {
        super.onStop();
        // 这里需要加一个isTimeSet来判断关闭时是否有更改时间，没有更改时间才走取消流程
        if (onCancelListener != null && !isTimeSet) {
            onCancelListener.onCancel(this);
        }
    }

    @Override
    public void setOnCancelListener(OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
    }

}
