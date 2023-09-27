package com.example.mediaplayproject.fragment.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.tools.LanguageChangeAdapter;
import com.example.mediaplayproject.bean.LanguageBean;
import com.example.mediaplayproject.bean.ToolsBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author wm
 */
public class ChangeLanguageFragment extends Fragment {

    private View myView;
    private Context mContext;
    private Button btnChangeEn, btnChangeZh;
    private List<LanguageBean> languageBeans = new ArrayList<>();
    private LanguageChangeAdapter languageChangeAdapter;
    private RecyclerView rvLanguageList;

    public ChangeLanguageFragment() {
    }

    public ChangeLanguageFragment(Context mContext) {
        this.mContext = mContext;
    }
    @SuppressLint("StaticFieldLeak")
    private static ChangeLanguageFragment instance;


    public static ChangeLanguageFragment getInstance(Context context) {
        if (instance == null) {
            instance = new ChangeLanguageFragment(context);
        }
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_change_language, container, false);
        initDate();
        initView();
        return myView;
    }


    private void initDate() {
        languageBeans.clear();
        ArrayList<String> languageNameList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.language_name_item)));
        ArrayList<String> languageList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.language_item)));
        ArrayList<String> countyList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.county_item)));
        // 这里应该做数量判断，后续加上
        for (int i = 0; i < languageList.size(); i++){
            languageBeans.add(new LanguageBean(languageNameList.get(i), languageList.get(i)));
        }
        DebugLog.debug("size " + languageBeans.size());
    }

    private void initView() {
        rvLanguageList = myView.findViewById(R.id.rv_change_language);
        languageChangeAdapter = new LanguageChangeAdapter(mContext, languageBeans);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        rvLanguageList.setLayoutManager(linearLayoutManager);
        rvLanguageList.setAdapter(languageChangeAdapter);

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