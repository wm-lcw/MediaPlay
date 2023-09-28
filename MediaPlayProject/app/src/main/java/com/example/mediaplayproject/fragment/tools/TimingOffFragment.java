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
public class TimingOffFragment extends Fragment {

    private View myView;
    private Context mContext;
    private ImageView ivBack;

    public TimingOffFragment() {
    }

    public TimingOffFragment(Context context){
        mContext = context;
    }

    private static TimingOffFragment timingOffFragment;
    public static TimingOffFragment getInstance(Context context) {
        if (timingOffFragment == null){
            timingOffFragment = new TimingOffFragment(context);
        }
        return timingOffFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_timing_off, container, false);
        return myView;
    }
}