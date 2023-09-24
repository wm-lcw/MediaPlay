package com.example.mediaplayproject.fragment.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;

import org.greenrobot.eventbus.EventBus;


/**
 * @author wm
 */
public class DiscoveryFragment extends Fragment {

    private Context mContext;
    private View myView;
    private Button btnChangeEn, btnChangeZh;

    public DiscoveryFragment() {
    }

    public DiscoveryFragment(Context mContext) {
        this.mContext = mContext;
    }

    private static DiscoveryFragment instance;
    public static DiscoveryFragment getInstance(Context context) {
        if (instance == null) {
            instance = new DiscoveryFragment(context);
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
        initView();
        return myView;
    }

    private void initView() {
        btnChangeEn = myView.findViewById(R.id.btn_change_en);
        btnChangeZh = myView.findViewById(R.id.btn_change_zh);
        btnChangeEn.setOnClickListener(mListener);
        btnChangeZh.setOnClickListener(mListener);
    }

    private final View.OnClickListener mListener = view -> {
        if (view == btnChangeEn) {
            SharedPreferencesUtil.putData(Constant.CURRENT_LANGUAGE, "en");
            SharedPreferencesUtil.putData(Constant.CURRENT_COUNTRY, "");
        } else if (view == btnChangeZh){
            SharedPreferencesUtil.putData(Constant.CURRENT_LANGUAGE, "zh");
            SharedPreferencesUtil.putData(Constant.CURRENT_COUNTRY, "CN");
        }
        Toast.makeText(mContext, R.string.change_language_tip,Toast.LENGTH_SHORT).show();
        // 这里调用Activity的recreate方法不可行，Fragment数据保存等工作太复杂
        // 暂时解法时切换语言后提示用户下次启动应用生效
//        EventBus.getDefault().post(Constant.SWITCH_LANGUAGE);
    };


}