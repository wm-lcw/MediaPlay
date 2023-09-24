package com.example.mediaplayproject.fragment.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.ToolsUtils;


/**
 * @author wm
 */
public class ToolsFragment extends Fragment {

    private Context mContext;
    private View myView;
    private LinearLayout llStatistics, llTimeOff, llChangeLanguage;

    public ToolsFragment(Context context){
        mContext = context;
    }
    private static ToolsFragment instance;
    public static ToolsFragment getInstance(Context context) {
        if (instance == null) {
            instance = new ToolsFragment(context);
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
        initView();
        return myView;
    }

    private void initView() {
        llStatistics = myView.findViewById(R.id.ll_tools_statistics);
        llTimeOff = myView.findViewById(R.id.ll_tools_time_off);
        llChangeLanguage = myView.findViewById(R.id.ll_tools_change_language);
        llStatistics.setOnClickListener(mListener);
        llTimeOff.setOnClickListener(mListener);
        llChangeLanguage.setOnClickListener(mListener);
    }

    private final View.OnClickListener mListener = view -> {
        if (view == llStatistics) {
            Intent intent = new Intent(Constant.CHANGE_FRAGMENT_ACTION);
            Bundle bundle = new Bundle();
            bundle.putString("fragment", Constant.STATISTICS_FRAGMENT_ACTION_FLAG);
            intent.putExtras(bundle);
            mContext.sendBroadcast(intent);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}