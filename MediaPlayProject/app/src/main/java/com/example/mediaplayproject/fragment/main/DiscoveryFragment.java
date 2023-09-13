package com.example.mediaplayproject.fragment.main;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.ToolsUtils;


/**
 * @author wm
 */
public class DiscoveryFragment extends Fragment {

    private View myView;
    private static DiscoveryFragment instance;
    public static DiscoveryFragment getInstance() {
        if (instance == null) {
            instance = new DiscoveryFragment();
        }
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_discovery, container, false);
        myView.setOnTouchListener((v, event) -> {
            ToolsUtils.getInstance().hideKeyboard(myView);
            return false;
        });
        return myView;
    }

}