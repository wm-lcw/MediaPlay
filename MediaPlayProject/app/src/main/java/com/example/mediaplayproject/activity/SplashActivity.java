package com.example.mediaplayproject.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.base.BasicActivity;

/**
 * @author wm
 */
public class SplashActivity extends BasicActivity {

    private TranslateAnimation translateAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    /**
     * 初始化
     */
    private void initView() {
        TextView tvTranslate = findViewById(R.id.tv_translate);

        tvTranslate.post(() -> {
            //通过post拿到的tvTranslate.getWidth()不会为0。
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
                    //动画结束时跳转到主页面
                    Intent intent =  new Intent(SplashActivity.this,MainActivity.class);
                    // MainActivity以SingleTask方式启动，且进入MainActivity之后清除SplashActivity记录
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        });

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }
}