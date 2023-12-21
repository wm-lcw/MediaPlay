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
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
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
    private View screenView;
    private MusicPlayService mPlayService;
    private TextView tvMusicName, tvMusicArtist;
    private ImageView ivPlayMode, ivPre, ivPlay, ivNext, ivFavorite;

    public LockScreenView(Context context) {
        super(context);
        init();
    }

    public LockScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        screenView = LayoutInflater.from(context).inflate(R.layout.lock_screen_view,this,true);
        init();
    }

    public LockScreenView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        screenView = LayoutInflater.from(context).inflate(R.layout.lock_screen_view,this,true);
        init();
    }

    public LockScreenView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        screenView = LayoutInflater.from(context).inflate(R.layout.lock_screen_view,this,true);
        init();
    }

    public void setWindowWidth(int mWindowWidth) {
        this.mWindowWidth = mWindowWidth;
    }

    public void setActivity(Activity activity) {
        this.mActivity = activity;
    }

    public void init(){
        bindView();
        initData();
    }

    private void bindView(){
        tvMusicName = screenView.findViewById(R.id.tv_music_name);
        tvMusicArtist = screenView.findViewById(R.id.tv_music_artist);

        ivPlayMode = screenView.findViewById(R.id.iv_loop);
        ivPre = screenView.findViewById(R.id.iv_pre);
        ivPlay = screenView.findViewById(R.id.iv_play);
        ivNext = screenView.findViewById(R.id.iv_next);
        ivFavorite = screenView.findViewById(R.id.iv_like);

        ivPlayMode.setOnClickListener(mListener);
        ivPre.setOnClickListener(mListener);
        ivPlay.setOnClickListener(mListener);
        ivNext.setOnClickListener(mListener);
        ivFavorite.setOnClickListener(mListener);
    }

    /**
     *  初始化数据和状态
     *  @author wm
     *  @createTime 2023/12/21 12:36
     */
    public void initData(){
        mPlayService = BasicApplication.getMusicService();

        if (mPlayService != null){
            tvMusicName.setText(mPlayService.getMusicTitle());
            tvMusicArtist.setText(mPlayService.getMusicArtist());
            ivPlay.setImageResource(mPlayService.isPlaying()
                    ? R.mipmap.media_pause : R.mipmap.media_play);

            ivPre.setEnabled(mPlayService.getPosition() != -1);
            ivPlay.setEnabled(mPlayService.getPosition() != -1);
            ivNext.setEnabled(mPlayService.getPosition() != -1);
            ivFavorite.setEnabled(mPlayService.getPosition() != -1);

            int playMode = mPlayService.getPlayMode();
            if (playMode == Constant.PLAY_MODE_SHUFFLE) {
                ivPlayMode.setImageResource(R.mipmap.media_shuffle);
            } else if (playMode == Constant.PLAY_MODE_SINGLE) {
                ivPlayMode.setImageResource(R.mipmap.media_single);
            } else {
                ivPlayMode.setImageResource(R.mipmap.media_loop);
            }
            boolean isLike = mPlayService.isFavorite();
            ivFavorite.setImageResource(isLike ? R.mipmap.ic_list_like_choose : R.mipmap.ic_list_like);
        } else {
            ivPre.setEnabled(false);
            ivPlay.setEnabled(false);
            ivNext.setEnabled(false);
            ivFavorite.setEnabled(false);
        }
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

    private final View.OnClickListener mListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view == ivPlayMode){
                mPlayService.changePlayMode();
                initData();
            } else if (view == ivPre){
                mPlayService.playPre();
            } else if (view == ivPlay){
                mPlayService.play();
            } else if (view == ivNext){
                mPlayService.playNext();
            } else if (view == ivFavorite){
                mPlayService.changFavoriteState();
                initData();
            }

        }
    };

}
