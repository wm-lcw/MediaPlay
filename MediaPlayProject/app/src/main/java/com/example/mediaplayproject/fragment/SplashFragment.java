package com.example.mediaplayproject.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

/**
 * @author wm
 */
public class SplashFragment extends Fragment {
    private TranslateAnimation translateAnimation;
    private View splashView;
    private Context mContext;
    private Handler mHandler;
    public SplashFragment(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }
    @SuppressLint("StaticFieldLeak")
    private static SplashFragment instance;

    public static SplashFragment getInstance(Context context, Handler handler) {
        if (instance == null) {
            instance = new SplashFragment(context, handler);
        }
        return instance;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        splashView = inflater.inflate(R.layout.fragment_splash, container, false);;
        initView();
        return splashView;
    }

    private void initView() {
        TextView tvTranslate = splashView.findViewById(R.id.tv_translate);

        tvTranslate.post(() -> {
            // 通过post拿到的tvTranslate.getWidth()不会为0。
            translateAnimation = new TranslateAnimation(0, tvTranslate.getWidth(), 0, 0);
            translateAnimation.setDuration(1000);
            translateAnimation.setFillAfter(true);
            tvTranslate.startAnimation(translateAnimation);

            //动画监听
            translateAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // 动画结束时跳转到主页面
                    Message msg = new Message();
                    msg.what = Constant.HANDLER_MESSAGE_START_MAIN_VIEW;
                    mHandler.sendMessage(msg);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        });

    }
}