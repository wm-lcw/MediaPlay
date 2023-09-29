package com.example.mediaplayproject.fragment.tools;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.mediaplayproject.R;


/**
 * @author wm
 */
public class WoodenFishFragment extends Fragment {

    private View myView;
    private Context mContext;
    private ImageView ivBack;

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

    }
}