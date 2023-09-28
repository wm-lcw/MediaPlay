package com.example.mediaplayproject.fragment.tools;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TimePicker;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.tools.TimingOffAdapter;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;
import com.example.mediaplayproject.view.CustomTimePickerDialog;

import java.util.ArrayList;
import java.util.Arrays;


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
    private CustomTimePickerDialog timePickerDialog;
    private int totalMin = 0;

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

    /**
     *  初始化数据
     *  @author wm
     *  @createTime 2023/9/28 16:43
     */
    private void initDate() {
        timingOffItemNameList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.timing_off_title_item)));
    }

    /**
     *  初始化UI资源
     *  @author wm
     *  @createTime 2023/9/28 16:43
     */
    private void initView() {
        ivBack = myView.findViewById(R.id.iv_timing_off_view_back);
        ivBack.setOnClickListener(mListen);
        rvTimingOff = myView.findViewById(R.id.rv_timing_off);
        timingOffAdapter = new TimingOffAdapter(mContext, timingOffItemNameList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        rvTimingOff.setLayoutManager(linearLayoutManager);
        rvTimingOff.setAdapter(timingOffAdapter);
        timingOffAdapter.setTimingOffAdapterListener(this);

        // 创建时间选择器对话框,最后一个参数代表是否使用 24 小时制
        timePickerDialog = new CustomTimePickerDialog( mContext,
            new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
                // 处理选择的时间,若识别到时间有更改才会执行,相当于确认按钮
                if (selectedHour >= 0){
                    totalMin = selectedHour * 60 + selectedMinute;
                }
                DebugLog.debug("hour:" + selectedHour + "; min:" + selectedMinute + "; totalMin:"+totalMin);
                setTimingOffTime(timingOffItemNameList.get(timingOffItemNameList.size()-1),totalMin);
            }
        }, 0, 0, true);

        timePickerDialog.setOnCancelListener(dialog -> {
            // 若自定义时间取消，则设为关闭状态
            setTimingOffTime(timingOffItemNameList.get(0),timingOffItemTimeList[0]);
        });
    }

    private View.OnClickListener mListen = v -> {
        if (v == ivBack) {
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        }
    };

    @Override
    public void onClickItem(int position) {
        // 这里设定了第一项是关闭，最后一项是自定义时间
        if (position == timingOffItemNameList.size() - 1) {
            // 自定义时间, 显示时间选择器对话框
            timePickerDialog.setTimeSet(false);
            timePickerDialog.show();
        } else {
            setTimingOffTime(timingOffItemNameList.get(position),timingOffItemTimeList[position]);
        }
    }

    /**
     *  更新定时关闭的状态和时间
     *  @author wm
     *  @createTime 2023/9/28 16:41
     * @param type: 类型
     * @param time: 时间
     */
    @SuppressLint("NotifyDataSetChanged")
    private void setTimingOffTime(String type, int time){
        if (time <= 0){
            // 设定的时间小于等于0，则设为关闭状态
            SharedPreferencesUtil.putData(Constant.TIMING_OFF_TYPE, timingOffItemNameList.get(0));
            SharedPreferencesUtil.putData(Constant.TIMING_OFF_TIME, 0);
        } else {
            SharedPreferencesUtil.putData(Constant.TIMING_OFF_TYPE, type);
            SharedPreferencesUtil.putData(Constant.TIMING_OFF_TIME, time);
        }
        timingOffAdapter.notifyDataSetChanged();
        Intent intent = new Intent(Constant.CHANGE_TIMING_OFF_TIME_ACTION);
        mContext.sendBroadcast(intent);
    }
}