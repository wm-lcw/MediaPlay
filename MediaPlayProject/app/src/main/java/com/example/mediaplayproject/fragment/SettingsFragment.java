package com.example.mediaplayproject.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;


/**
 * @author wm
 */
public class SettingsFragment extends Fragment {

    private static SettingsFragment mSettingsFragment;
    private Context mContext;
    private View mView;
    private ImageView ivBack;
    private Switch swLog, swLockScreen;

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
        initView();
        initData();
        return mView;
    }

    private void initView() {
        ivBack = mView.findViewById(R.id.iv_settings_back);
        swLog = mView.findViewById(R.id.sw_log);


        ivBack.setOnClickListener(mListener);
        swLog.setOnCheckedChangeListener(mCheckedChangeListener);
    }

    private void initData() {
            // 打印控制逻辑
            boolean isOpenLog = (Boolean) SharedPreferencesUtil.getData(Constant.LOG_SWITCH,true);
            DebugLog.debug("isOpenLog " + isOpenLog);
            swLog.setChecked(isOpenLog);
    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivBack) {
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        }
    };

    private final CompoundButton.OnCheckedChangeListener mCheckedChangeListener = (buttonView, isChecked) -> {
        if (buttonView == swLog && buttonView.isPressed()){
            boolean result = SharedPreferencesUtil.putData(Constant.LOG_SWITCH, isChecked);
            DebugLog.debug("set log switch " + isChecked  + "; result: " + result);
            if (result){
                Toast.makeText(mContext,"设置成功，重启生效！", Toast.LENGTH_LONG).show();
            }
        }
    };
}