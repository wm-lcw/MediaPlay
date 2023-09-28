package com.example.mediaplayproject.fragment.tools;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.tools.LanguageChangeAdapter;
import com.example.mediaplayproject.adapter.tools.TimingOffAdapter;
import com.example.mediaplayproject.bean.LanguageBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author wm
 */
public class TimingOffFragment extends Fragment {

    private View myView;
    private Context mContext;
    private ImageView ivBack;
    private TimingOffAdapter timingOffAdapter;
    private RecyclerView rvTimingOff;
    private ArrayList<String> timingOffItemNameList;

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
        initDate();
        initView();
        return myView;
    }

    private void initDate() {
        timingOffItemNameList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.timing_off_title_item)));

    }

    private void initView() {
        rvTimingOff = myView.findViewById(R.id.rv_timing_off);
        timingOffAdapter = new TimingOffAdapter(mContext, timingOffItemNameList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        rvTimingOff.setLayoutManager(linearLayoutManager);
        rvTimingOff.setAdapter(timingOffAdapter);
    }
}