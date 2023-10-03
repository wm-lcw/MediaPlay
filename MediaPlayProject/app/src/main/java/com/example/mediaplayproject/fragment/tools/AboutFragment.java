package com.example.mediaplayproject.fragment.tools;

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
public class AboutFragment extends Fragment {

    private Context mContext;
    private View myView;

    public AboutFragment() {
    }

    public AboutFragment(Context mContext) {
        this.mContext = mContext;
    }

    private static AboutFragment aboutFragment;
    public static AboutFragment getInstance(Context context) {
        if (aboutFragment == null){
            aboutFragment = new AboutFragment(context);
        }
        return aboutFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_about, container, false);;
        return myView;
    }
}