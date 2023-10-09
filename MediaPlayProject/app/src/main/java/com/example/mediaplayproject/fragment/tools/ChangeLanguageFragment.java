package com.example.mediaplayproject.fragment.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.MainActivity;
import com.example.mediaplayproject.adapter.tools.LanguageChangeAdapter;
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.LanguageBean;
import com.example.mediaplayproject.bean.ToolsBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.MultiLanguageUtil;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


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
    private final static int HANDLER_MESSAGE_RESTART = 1;

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
            // 返回主页
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        }
    };

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onClickItem(int position) {
        String saveLanguage = languageList.get(position);
        String saveCounty = countyList.get(position);
//        SharedPreferencesUtil.putData(Constant.CURRENT_LANGUAGE, saveLanguage);
//        SharedPreferencesUtil.putData(Constant.CURRENT_COUNTRY, saveCounty);
        changeLanguage(saveLanguage, saveCounty);
        languageChangeAdapter.notifyDataSetChanged();
    }

    /**
     *  修改应用内语言设置
     *  @author wm
     *  @createTime 2023/10/8 16:25
     * @param language:  语言
     * @param area: 地区
     */
    private void changeLanguage(String language, String area) {
        if (TextUtils.isEmpty(language) && TextUtils.isEmpty(area)) {
            // 如果语言和地区都是空，那么跟随系统语言
            SharedPreferencesUtil.putData(Constant.CURRENT_LANGUAGE, "");
            SharedPreferencesUtil.putData(Constant.CURRENT_COUNTRY, "");
        } else {
            // 不为空则修改app语言，最后一个参数含义是：是否把语言信息保存到sp中
            Locale newLocale = new Locale(language, area);
            MultiLanguageUtil.changeAppLanguage(BasicApplication.getContext(), newLocale, true);
        }
        // 需要延迟一点再重启app，否者信息还没保存就重启了，导致设置无作用
        handler.sendEmptyMessageDelayed(HANDLER_MESSAGE_RESTART, 300);
    }

    final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("ResourceAsColor")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_RESTART) {
                // 重启app,这一步一定要加上，如果不重启app，可能打开新的页面显示的语言会不正确
                Intent intent = new Intent(BasicApplication.getContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                BasicApplication.getContext().startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }};
}