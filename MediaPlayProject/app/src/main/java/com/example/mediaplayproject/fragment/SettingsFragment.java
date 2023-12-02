package com.example.mediaplayproject.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediaplayproject.R;


/**
 * @author wm
 */
public class SettingsFragment extends Fragment {

    private static SettingsFragment mSettingsFragment;
    private Context mContext;
    private View mView;

    public SettingsFragment(Context context) {
        mContext = context;
    }

    public static SettingsFragment getInstance(Context context){
        if (mSettingsFragment == null){
            mSettingsFragment = new SettingsFragment(context);
        }
        return mSettingsFragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_settings, container, false);
        return mView;
    }
}