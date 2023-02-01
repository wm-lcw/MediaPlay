package com.example.mediaplayproject.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SearchFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author wm
 */
public class MusicPlayActivity extends AppCompatActivity {

    private ImageView ivMediaLoop, ivMediaPre, ivMediaPlay, ivMediaNext, ivMediaList, ivCloseListView;
    private SeekBar sbVolume, sbProgress;
    private boolean playState = false;
    private Context mContext;
    private List<MediaFileBean> musicInfo = new ArrayList<>();
    private LinearLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);
        mContext = this;
        updateMusicFiles();
        initData();
    }

    private void updateMusicFiles() {
        SearchFiles mSearcherFiles = SearchFiles.getInstance(mContext);
        musicInfo = mSearcherFiles.getMusicInfo();
        if (musicInfo.size() > 0) {
            Iterator<MediaFileBean> iterator = musicInfo.iterator();
            while (iterator.hasNext()) {
                MediaFileBean mediaFileBean = iterator.next();
                DebugLog.debug(mediaFileBean.getTitle() + " -- " + mediaFileBean.getData());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ivMediaPlay.setImageResource(playState ? R.mipmap.media_pause : R.mipmap.media_play);
    }

    private void initData() {
        ivMediaLoop = findViewById(R.id.bt_loop);
        ivMediaPre = findViewById(R.id.bt_pre);
        ivMediaPlay = findViewById(R.id.bt_play);
        ivMediaNext = findViewById(R.id.bt_next);
        ivMediaList = findViewById(R.id.bt_list);
        sbVolume = findViewById(R.id.sb_volume);
        sbProgress = findViewById(R.id.sb_progress);

        ivMediaLoop.setOnClickListener(mListener);
        ivMediaPre.setOnClickListener(mListener);
        ivMediaPlay.setOnClickListener(mListener);
        ivMediaNext.setOnClickListener(mListener);
        ivMediaList.setOnClickListener(mListener);

        sbVolume.setOnSeekBarChangeListener(mSeekBarListener);
        sbProgress.setOnSeekBarChangeListener(mSeekBarListener);


    }

    private View.OnClickListener mListener = new View.OnClickListener() {
        @SuppressLint("ResourceType")
        @Override
        public void onClick(View view) {
            if (view == ivMediaLoop) {
                DebugLog.debug("onclick ivMediaLoop");
            } else if (view == ivMediaPre) {
                DebugLog.debug("onclick ivMediaPre");
            } else if (view == ivMediaPlay) {
                DebugLog.debug("onclick ivMediaPlay playState is " + playState);
                if (playState) {
                    playState = false;
                    ivMediaPlay.setImageResource(R.mipmap.media_play);
                } else {
                    playState = true;
                    ivMediaPlay.setImageResource(R.mipmap.media_pause);
                }
            } else if (view == ivMediaNext) {
                DebugLog.debug("onclick ivMediaNext");
            } else if (view == ivMediaList) {
                DebugLog.debug("onclick ivMediaList");
                createFloatView();
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == sbVolume) {
                DebugLog.debug("volume progress is " + progress);
            } else if (seekBar == sbProgress) {
                DebugLog.debug("music progress is " + progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager)getApplication().getSystemService(Context.WINDOW_SERVICE);
        DebugLog.debug("mWindowManager--->" + mWindowManager);
        //设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口,不设置这个flag的话，home页的划屏会有问题
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wmParams.flags = flags;
        //调整悬浮窗显示的停靠位置为右侧底部
        wmParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        // 以屏幕右下角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 30;
        wmParams.y = 400;

        //  设置悬浮窗口长宽数据
        wmParams.width = 600;
        wmParams.height = 1100;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.layout_music_lit, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);

        //浮动窗口关闭按钮
        ivCloseListView = mFloatLayout.findViewById(R.id.iv_list_close);
        ivCloseListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DebugLog.debug("close list");
                mWindowManager.removeView(mFloatLayout);
            }
        });


    }

}