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
import android.widget.Button;
import android.widget.ImageView;
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
public class ChangeLanguageFragment extends Fragment implements LanguageChangeAdapter.LanguageChangeAdapterListener {

    private View myView;
    private Context mContext;
    private ImageView ivBack;
    private List<LanguageBean> languageBeans = new ArrayList<>();
    private LanguageChangeAdapter languageChangeAdapter;
    private RecyclerView rvLanguageList;
    private ArrayList<String> languageNameList, languageList, countyList;

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
        languageNameList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.language_name_item)));
        languageList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.language_item)));
        countyList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.county_item)));
        languageBeans.clear();
        // 这里应该做数量判断，后续加上
        for (int i = 0; i < languageList.size(); i++){
            languageBeans.add(new LanguageBean(languageNameList.get(i), languageList.get(i)));
        }
        DebugLog.debug("size " + languageBeans.size());
    }

    private void initView() {
        ivBack = myView.findViewById(R.id.iv_change_language_view_back);
        ivBack.setOnClickListener(mListener);
        rvLanguageList = myView.findViewById(R.id.rv_change_language);
        languageChangeAdapter = new LanguageChangeAdapter(mContext, languageBeans);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        rvLanguageList.setLayoutManager(linearLayoutManager);
        rvLanguageList.setAdapter(languageChangeAdapter);
        languageChangeAdapter.setLanguageChangeAdapterListener(this);

    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivBack) {
            Intent intent = new Intent(Constant.RETURN_MAIN_VIEW_ACTION);
            mContext.sendBroadcast(intent);
        }
    };

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onClickItem(int position) {
        String saveLanguage = languageList.get(position);
        String saveCounty = countyList.get(position);
        SharedPreferencesUtil.putData(Constant.CURRENT_LANGUAGE, saveLanguage);
        SharedPreferencesUtil.putData(Constant.CURRENT_COUNTRY, saveCounty);
        Toast.makeText(mContext, R.string.change_language_tip,Toast.LENGTH_SHORT).show();
        languageChangeAdapter.notifyDataSetChanged();

        // 这里调用Activity的recreate方法不可行，Fragment数据保存等工作太复杂
        // 暂时解法时切换语言后提示用户下次启动应用生效
//        EventBus.getDefault().post(Constant.SWITCH_LANGUAGE);
    }
}