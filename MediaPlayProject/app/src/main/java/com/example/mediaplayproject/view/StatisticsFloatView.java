package com.example.mediaplayproject.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.mediaplayproject.utils.DebugLog;

/**
 * @author wm
 * @Date 2023/11/18 9:19
 * @Created by wm
 */
public class StatisticsFloatView extends LinearLayout {

    private StatisticsFloatViewCallback mFloatViewCallback;

    public void setFloatViewCallback(StatisticsFloatViewCallback floatViewCallback) {
        mFloatViewCallback = floatViewCallback;
    }

    public StatisticsFloatView(Context context, int layoutId) {
        super(context);
        inflate(context, layoutId, this);
    }

    /**
     * 这个构造方法要加上，否则可能会出错
     * */
    public StatisticsFloatView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 这个构造方法要加上，否则可能会出错
     * */
    public StatisticsFloatView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_OUTSIDE){
            DebugLog.info("outside --");
            mFloatViewCallback.onClickOutside();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    public interface StatisticsFloatViewCallback{
        /**
         *  悬浮窗点击外部回调事件
         *  @author wm
         *  @createTime 2023/11/18 9:34
         */
        void onClickOutside();
    }
}
