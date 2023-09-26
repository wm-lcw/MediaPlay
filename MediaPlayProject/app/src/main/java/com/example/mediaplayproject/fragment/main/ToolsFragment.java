package com.example.mediaplayproject.fragment.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.tools.AllToolsItemListAdapter;
import com.example.mediaplayproject.adapter.tools.ShortcutToolsItemListAdapter;
import com.example.mediaplayproject.bean.ToolsBean;
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

    private RecyclerView rvShortcutTools, rvAllTools;
    private List<ToolsBean> allToolsBeanList = new ArrayList<>();
    private List<ToolsBean> shortcutToolsBeanList = new ArrayList<>();
    private List<ToolsBean> tempToolsBeanList = new ArrayList<>();
    private AllToolsItemListAdapter allToolsItemListAdapter;
    private ShortcutToolsItemListAdapter shortcutToolsItemListAdapter;

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
            allToolsBeanList.add(toolsBean);
        }

        shortcutToolsBeanList.add(allToolsBeanList.get(0));
        shortcutToolsBeanList.add(allToolsBeanList.get(1));

    }

    private void initView() {
        
        try {
            rvAllTools = myView.findViewById(R.id.rv_all_tools_item);
            rvShortcutTools = myView.findViewById(R.id.rv_shortcut_tools_item);

            allToolsItemListAdapter = new AllToolsItemListAdapter(mContext, allToolsBeanList);
            rvAllTools.setAdapter(allToolsItemListAdapter);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext,3);
            rvAllTools.setLayoutManager(gridLayoutManager);

            shortcutToolsItemListAdapter = new ShortcutToolsItemListAdapter(mContext, shortcutToolsBeanList);
            rvShortcutTools.setAdapter(shortcutToolsItemListAdapter);
            GridLayoutManager gridLayoutManager2 = new GridLayoutManager(mContext,3);
            rvShortcutTools.setLayoutManager(gridLayoutManager2);

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