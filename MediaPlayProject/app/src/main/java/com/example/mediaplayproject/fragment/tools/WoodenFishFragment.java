package com.example.mediaplayproject.fragment.tools;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.ToolsUtils;



/**
 * @author wm
 */
public class WoodenFishFragment extends Fragment {

    private View myView;
    private Context mContext;
    private ImageView ivBack, ivWoodenFish;
    private Animation animation;
    private RelativeLayout woodenFishRootView;
    private TextView textView;
    private static int[] colorList = {Color.WHITE,Color.YELLOW,Color.GREEN,Color.RED,Color.GRAY,Color.BLUE};

    public WoodenFishFragment() {
    }


    private static WoodenFishFragment woodenFishFragment;

    public WoodenFishFragment(Context mContext) {
        this.mContext = mContext;
    }

    public static WoodenFishFragment getInstance(Context context) {
        if (woodenFishFragment == null) {
            woodenFishFragment = new WoodenFishFragment(context);
        }
        return woodenFishFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_wooden_fish, container, false);
        initView();
        return myView;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        woodenFishRootView = myView.findViewById(R.id.rl_wooden_fish_root_view);
        ivBack = myView.findViewById(R.id.iv_wooden_fish_view_back);
        ivWoodenFish = myView.findViewById(R.id.iv_wooden_fish);
        ivBack.setOnClickListener(mListen);
        animation = AnimationUtils.loadAnimation(mContext, R.anim.wooden_fish_icon_click_animation);
        ivWoodenFish.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    ivWoodenFish.startAnimation(animation);
                    // 使用根布局addView添加一个TextView
                    woodenFishRootView.addView(textView = new TextView(mContext));
                    textView.setText("功德+1");
                    textView.setTextSize(20);
                    int color = (int)(Math.random()*colorList.length);
                    textView.setTextColor(colorList[color]);
                    //实现轨迹动画的代码
                    AnimatorSet animSet = new AnimatorSet();
                    // X轴的移动
                    ObjectAnimator anim1 = ObjectAnimator.ofFloat(textView, "translationX", event.getRawX(), event.getRawX());
                    // Y轴的移动
                    ObjectAnimator anim2 = ObjectAnimator.ofFloat(textView, "translationY", event.getRawY(), 0f);
                    //移动过程中逐渐透明
                    ObjectAnimator anim3 = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);

                    AnimatorSet.Builder builder = animSet.play(anim2);
                    builder.with(anim1).with(anim2).with(anim3);//三个动画一起执行
                    animSet.setDuration(3500);//整个过程持续3.5s
                    animSet.start(); // 开始播放属性动画
                    return true;
                }
                return false;
            }
        });
    }

    private View.OnClickListener mListen = v -> {
        if (v == ivBack) {
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        }
    };

}