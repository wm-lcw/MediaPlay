package com.example.mediaplayproject.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.DebugLog;

/**
 * @author wm
 * @Classname LockScreenView
 * @Description 自定义的锁屏界面
 * @Version 1.0.0
 * @Date 2023/12/20 15:00
 * @Created by wm
 */
public class LockScreenView extends RelativeLayout {

    private float mStartX, mWindowWidth;
    private Activity mActivity;

    public LockScreenView(Context context) {
        super(context);
    }

    public LockScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.lock_screen_view,this,true);
    }

    public LockScreenView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.lock_screen_view,this,true);
    }

    public LockScreenView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.lock_screen_view,this,true);
    }

    public void setWindowWidth(int mWindowWidth) {
        this.mWindowWidth = mWindowWidth;
    }

    public void setActivity(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final float x = ev.getX();
        DebugLog.debug("x" + x);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                moveContent(x);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleTouchResult(x);
                break;
            default:
                break;
        }
        return true;
    }

    @SuppressLint("ObjectAnimatorBinding")
    private void moveContent(float x) {
        float offsetX = x - mStartX;
        if (offsetX < 0f) {
            offsetX = 0f;
        }
        // 内容的偏移量
        setTranslationX(offsetX);
    }

    private void handleTouchResult(float destination) {
        float offsetX = destination - mStartX;
        DebugLog.debug("offsetX = " +offsetX + " ;  mWindowWidth " + mWindowWidth);
        if (offsetX > mWindowWidth * 0.3) {
            // 超过阈值（屏幕宽度的0.3倍），结束锁屏activity
            handleTouchResult(mWindowWidth - getLeft(), true);
        } else {
            // 否则内容回到原位
            handleTouchResult(-getLeft(), false);
        }
    }

    /**
     *  移动结束后的View操作
     *  @author wm
     *  @createTime 2023/12/20 16:19
     * @param destination: 移动的距离
     * @param finishActivity: 是否结束Activity
     */
    @SuppressLint("ObjectAnimatorBinding")
    private void handleTouchResult(float destination, boolean finishActivity) {
        DebugLog.debug("xx " + destination + "; finish " + finishActivity);
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationX", destination);
        animator.setDuration(250).start();
        if (finishActivity) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    DebugLog.debug("unlock----");
                    mActivity.finish();
                }
            });
        }
    }

}
