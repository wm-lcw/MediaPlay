package com.example.mediaplayproject.view;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.DebugLog;

/**
 * @author wm
 * @Date 2023/11/18 9:19
 * @Created by wm
 */
public class PlayControllerView extends LinearLayout {

    private View playControllerView;
    private GestureDetector gestureDetector;
    private Scroller scroller;
    private PlayControllerCallback mControllerCallback;
    private float mStartX;

    /**
     * 这个构造方法要加上，否则可能会出错
     * */
    public PlayControllerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        playControllerView = LayoutInflater.from(context).inflate(R.layout.layout_play_controller_view,this,true);
        init(context);
    }

    /**
     * 这个构造方法要加上，否则可能会出错
     * */
    public PlayControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        playControllerView = LayoutInflater.from(context).inflate(R.layout.layout_play_controller_view,this,true);
        init(context);
    }

    private void init(Context context) {
        DebugLog.debug("");
//        useScroller(context);
    }
    
//    /**
//     *  使用GestureDetector手势监听的方式来监听左右移动，使用scroller来移动View（这里View没有移动成功）
//     *  @author wm
//     *  @createTime 2024/1/10 14:54
//     * @param context:
//     */
//    private void useScroller(Context context){
//        gestureDetector = new GestureDetector(context, new MyGestureListener());
//        scroller = new Scroller(context);
//        setOnTouchListener(new OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                return gestureDetector.onTouchEvent(motionEvent);
//            }
//        });
//        // 这里必须设置setLongClickable为true 否则监听不到手势！！！
//        setLongClickable(true);
//    }
//
//    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
//        private static final int SWIPE_THRESHOLD = 150;
//        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
//
//        @Override
//        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//            float diffX = e2.getX() - e1.getX();
//            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
//                if (diffX > 0) {
//                    // 向右滑动，切换到上一首歌曲
//                    onSwipeRight();
//                } else {
//                    // 向左滑动，切换到下一首歌曲
//                    onSwipeLeft();
//                }
//                return true;
//            } else {
//                // 滑动距离不足，回到原始位置
//                smoothScrollToOriginalPosition();
//            }
//            return false;
//        }
//    }
//
//    private void smoothScrollToOriginalPosition() {
//        DebugLog.debug("");
//        int startX = getScrollX();
//        int startY = getScrollY();
//        // 计算回到原始位置的偏移量
//        int dx = -startX;
//        int dy = -startY;
//        // 动画持续时间
//        int duration = 500;
//
//        scroller.startScroll(startX, startY, dx, dy, duration);
//        // 通知 View 重新绘制
//        invalidate();
//    }
//
//    @Override
//    public void computeScroll() {
//        DebugLog.debug("---0");
//        if (scroller.computeScrollOffset()) {
//            DebugLog.debug("---0");
//            scrollTo(scroller.getCurrX(), scroller.getCurrY());
//            // 通知 View 重新绘制
//            postInvalidate();
//        }
//    }

    public void setControllerCallback(PlayControllerCallback controllerCallback) {
        mControllerCallback = controllerCallback;
    }

    public interface PlayControllerCallback{
        /**
         *  控制切换歌曲
         *  @author wm
         *  @createTime 2024/1/10 14:18
         *  @param isNext: true：下一曲；  false：上一曲
         */
        void controlPlayNextOrPre(boolean isNext);
    }

    private void onSwipeRight() {
        // 处理向右滑动的逻辑，例如切换到上一首歌曲
        DebugLog.debug("---");
        mControllerCallback.controlPlayNextOrPre(false);
    }

    private void onSwipeLeft() {
        // 处理向左滑动的逻辑，例如切换到下一首歌曲
        DebugLog.debug("---");
        mControllerCallback.controlPlayNextOrPre(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final float x = ev.getX();
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

    /**
     *  移动view
     *  @author wm
     *  @createTime 2024/1/10 15:09
     *  @param destination: 偏移量
     */
    @SuppressLint("ObjectAnimatorBinding")
    private void moveContent(float destination) {
        float offsetX = destination - mStartX;
        // 内容的偏移量
        setTranslationX(offsetX);
    }

    /**
     *  手势抬起后的处理
     *  @author wm
     *  @createTime 2024/1/10 15:10
     *  @param destination:
     */
    private void handleTouchResult(float destination) {
        float offsetX = destination - mStartX;
        if (Math.abs(offsetX) > 100) {
            // 如果移动距离足够了，就处理切换歌曲的事件
            if (offsetX > 0) {
                // 向右滑动，切换到上一首歌曲
                onSwipeRight();
            } else {
                // 向左滑动，切换到下一首歌曲
                onSwipeLeft();
            }
        }
        // 内容回到原位
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationX", -getLeft());
        animator.setDuration(250).start();
    }
}
