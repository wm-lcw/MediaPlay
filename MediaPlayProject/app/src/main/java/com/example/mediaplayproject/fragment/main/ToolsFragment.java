package com.example.mediaplayproject.fragment.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.ToolsItemListAdapter;
import com.example.mediaplayproject.bean.ToolsBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author wm
 */
public class ToolsFragment extends Fragment {

    private Context mContext;
    private View myView;
    private ArrayList<String> itemTitleList;
    private static final  int[] TOOLS_ITEM_ICON_LIST = {
      R.mipmap.ic_tools_history_record_blue, R.mipmap.ic_tools_timing_blue, R.mipmap.ic_tools_change_language_blue,
      R.mipmap.ic_tools_wooden_blue, R.mipmap.ic_tools_my_blue, R.mipmap.ic_tools_settings_blue,
    };

    private RecyclerView rvToolsItem;
    private List<ToolsBean> toolsBeanList = new ArrayList<>();
    private ToolsItemListAdapter toolsItemListAdapter;

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
        initItemBean();
        initView();
        return myView;
    }

    private void initItemBean() {
        itemTitleList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.tools_item_title)));
        // 这里应该做数量判断，后续加上
        for (int i = 0; i < itemTitleList.size(); i++){
            ToolsBean toolsBean = new ToolsBean(i, itemTitleList.get(i), TOOLS_ITEM_ICON_LIST[i]);
            toolsBeanList.add(toolsBean);
        }
    }

    private void initView() {

        for (int i = 0; i < toolsBeanList.size(); i++){
            DebugLog.debug(toolsBeanList.get(i).getToolsName() + "; " + toolsBeanList.get(i).getToolsIconId());
        }

        try {
            rvToolsItem = myView.findViewById(R.id.rv_tools_item);
            DebugLog.debug("list " + toolsBeanList.size());
            toolsItemListAdapter = new ToolsItemListAdapter(mContext, toolsBeanList);
            rvToolsItem.setAdapter(toolsItemListAdapter);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext,3);
            rvToolsItem.setLayoutManager(gridLayoutManager);

        } catch (Exception exception){
            DebugLog.debug(exception.getMessage());
        }



    }

//    private final View.OnClickListener mListener = view -> {
//        if (view == llStatistics) {
//            Intent intent = new Intent(Constant.CHANGE_FRAGMENT_ACTION);
//            Bundle bundle = new Bundle();
//            bundle.putString("fragment", Constant.STATISTICS_FRAGMENT_ACTION_FLAG);
//            intent.putExtras(bundle);
//            mContext.sendBroadcast(intent);
//        }
//    };

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}