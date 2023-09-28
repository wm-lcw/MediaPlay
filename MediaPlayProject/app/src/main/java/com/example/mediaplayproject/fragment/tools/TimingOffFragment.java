package com.example.mediaplayproject.fragment.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.LanguageBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author wm
 */
public class TimingOffFragment extends Fragment implements TimingOffAdapter.TimingOffAdapterListener {

    private View myView;
    private Context mContext;
    private ImageView ivBack;
    private TimingOffAdapter timingOffAdapter;
    private RecyclerView rvTimingOff;
    private ArrayList<String> timingOffItemNameList;
    private static final int[] timingOffItemTimeList = {0, 10, 20, 30, 60};

    public TimingOffFragment() {
    }

    public TimingOffFragment(Context context) {
        mContext = context;
    }

    private static TimingOffFragment timingOffFragment;

    public static TimingOffFragment getInstance(Context context) {
        if (timingOffFragment == null) {
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
        ivBack = myView.findViewById(R.id.iv_timing_off_view_back);
        ivBack.setOnClickListener(mListen);
        rvTimingOff = myView.findViewById(R.id.rv_timing_off);
        timingOffAdapter = new TimingOffAdapter(mContext, timingOffItemNameList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        rvTimingOff.setLayoutManager(linearLayoutManager);
        rvTimingOff.setAdapter(timingOffAdapter);
        timingOffAdapter.setTimingOffAdapterListener(this);
    }

    private View.OnClickListener mListen = v -> {
        if (v == ivBack) {
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        }
    };

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onClickItem(int position) {
        SharedPreferencesUtil.putData(Constant.TIMING_OFF_TYPE, timingOffItemNameList.get(position));
        // 这里设定了第一项是关闭，最后一项是自定义时间
        if (position == timingOffItemNameList.size() - 1) {
            // 自定义时间,这里需要唤出TimePicker
            SharedPreferencesUtil.putData(Constant.TIMING_OFF_TIME, 1);
        } else {
            SharedPreferencesUtil.putData(Constant.TIMING_OFF_TIME, timingOffItemTimeList[position]);
        }
        timingOffAdapter.notifyDataSetChanged();
        Intent intent = new Intent(Constant.CHANGE_TIMING_OFF_TIME_ACTION);
        mContext.sendBroadcast(intent);
    }
    
}