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
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
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
    private TextView tvWoodenFishTip,tvWoodenFishCount;
    private static int[] colorList = {Color.WHITE,Color.YELLOW,Color.GREEN,Color.RED,Color.GRAY,Color.BLUE};
    private long woodenFishCount = 0;
    String countText = "0";

    /**
     * 是否可点击的标志
     * */
    private boolean isClickable = true;
    /**
     * 限制点击的时间间隔
     * */
    private static final long CLICK_DELAY = 500;


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
    public void onResume() {
        super.onResume();
        woodenFishCount = (long) SharedPreferencesUtil.getData(Constant.WOODEN_FISH_COUNT, 0L);
        countText = mContext.getString(R.string.wooden_fish_count) + " " + woodenFishCount;
        tvWoodenFishCount.setText(countText);

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
        tvWoodenFishCount = myView.findViewById(R.id.tv_wooden_fish_count);

        ivBack.setOnClickListener(mListen);
        animation = AnimationUtils.loadAnimation(mContext, R.anim.wooden_fish_icon_click_animation);
        ivWoodenFish.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN){
                if (isClickable) {
                    // 执行按钮点击的操作
                    doClickWoodenFish(event);

                    // 设置为不可点击状态，并在一定时间后恢复可点击
                    isClickable = false;
                    ivWoodenFish.setEnabled(false);
                    ivWoodenFish.postDelayed(() -> {
                        isClickable = true;
                        ivWoodenFish.setEnabled(true);
                    }, CLICK_DELAY);
                }
                return true;
            }
            return false;
        });
    }

    /**
     *  处理点击木鱼图标的事件
     *  @author wm
     *  @createTime 2023/9/30 20:54
     * @param event: 触摸事件，主要用于获取触摸的坐标
     */
    private void doClickWoodenFish(MotionEvent event) {
        ivWoodenFish.startAnimation(animation);
        // 使用根布局addView添加一个TextView
        woodenFishRootView.addView(tvWoodenFishTip = new TextView(mContext));
        tvWoodenFishTip.setText(R.string.wooden_fish_Merit);
        tvWoodenFishTip.setTextSize(20);
        int color = (int)(Math.random()*colorList.length);
        tvWoodenFishTip.setTextColor(colorList[color]);
        //实现轨迹动画的代码
        AnimatorSet animSet = new AnimatorSet();
        // X轴的移动
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(tvWoodenFishTip, "translationX", event.getRawX(), event.getRawX());
        // Y轴的移动
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(tvWoodenFishTip, "translationY", event.getRawY(), 0f);
        //移动过程中逐渐透明
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(tvWoodenFishTip, "alpha", 1f, 0f);

        AnimatorSet.Builder builder = animSet.play(anim2);
        //三个动画一起执行
        builder.with(anim1).with(anim2).with(anim3);
        //整个过程持续3.5s
        animSet.setDuration(3500);
        // 开始播放属性动画
        animSet.start();

        // 播放点击音效
        ToolsUtils.getInstance().audioPlay(mContext);

        // 保存、刷新计数值
        woodenFishCount++;
        SharedPreferencesUtil.putData(Constant.WOODEN_FISH_COUNT, woodenFishCount);
        countText = mContext.getString(R.string.wooden_fish_count) + " " + woodenFishCount;
        tvWoodenFishCount.setText(countText);
    }

    private View.OnClickListener mListen = v -> {
        if (v == ivBack) {
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        }
    };

}