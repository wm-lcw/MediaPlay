package com.example.mediaplayproject.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
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
public class ToolsFragment extends Fragment {
    private View myView;
    private static ToolsFragment instance;
    public static ToolsFragment getInstance() {
        if (instance == null) {
            instance = new ToolsFragment();
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
        myView = inflater.inflate(R.layout.fragment_tools, container, false);

        myView.setOnTouchListener((v, event) -> {
            ToolsUtils.getInstance().hideKeyboard(myView);
            return false;
        });
        return myView;
    }

}