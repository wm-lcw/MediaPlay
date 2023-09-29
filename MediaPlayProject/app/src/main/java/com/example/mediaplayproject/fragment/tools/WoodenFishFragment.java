package com.example.mediaplayproject.fragment.tools;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.ToolsUtils;


/**
 * @author wm
 */
public class WoodenFishFragment extends Fragment {

    private View myView;
    private Context mContext;
    private ImageView ivBack,ivWoodenFish;
    private Animation animation;

    public WoodenFishFragment() {
    }


    private static WoodenFishFragment woodenFishFragment;

    public WoodenFishFragment(Context mContext) {
        this.mContext = mContext;
    }

    public static WoodenFishFragment getInstance(Context context) {
        if (woodenFishFragment == null){
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

    private void initView() {
        ivBack = myView.findViewById(R.id.iv_wooden_fish_view_back);
        ivWoodenFish = myView.findViewById(R.id.iv_wooden_fish);
        ivBack.setOnClickListener(mListen);
        ivWoodenFish.setOnClickListener(mListen);
        animation = AnimationUtils.loadAnimation(mContext, R.anim.wooden_fish_icon_click_animation);

    }

    private View.OnClickListener mListen = v -> {
        if (v == ivBack) {
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        } else if (v == ivWoodenFish){
            ivWoodenFish.startAnimation(animation);
        }
    };
}