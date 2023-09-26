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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.tools.AllToolsItemListAdapter;
import com.example.mediaplayproject.adapter.tools.ShortcutToolsItemListAdapter;
import com.example.mediaplayproject.bean.ToolsBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * @author wm
 */
public class ToolsFragment extends Fragment implements AllToolsItemListAdapter.AllToolsItemListAdapterListener,
        ShortcutToolsItemListAdapter.ShortcutToolsItemListAdapterListener {

    private Context mContext;
    private View myView;
    private ArrayList<String> itemTitleList;
    private static final  int[] TOOLS_ITEM_ICON_LIST = {
      R.mipmap.ic_tools_history_record_blue, R.mipmap.ic_tools_timing_blue, R.mipmap.ic_tools_change_language_blue,
      R.mipmap.ic_tools_wooden_blue, R.mipmap.ic_tools_my_blue, R.mipmap.ic_tools_settings_blue,
    };

    private static final int MAX_SHORTCUT_TOOLS_NUM = 3;
    private RecyclerView rvShortcutTools, rvAllTools;
    private List<ToolsBean> allToolsBeanList = new ArrayList<>();
    private List<ToolsBean> shortcutToolsBeanList = new ArrayList<>();
    private AllToolsItemListAdapter allToolsItemListAdapter;
    private ShortcutToolsItemListAdapter shortcutToolsItemListAdapter;
    private ToolsBean addToolsBean;

    private LinearLayout llSaveView;
    private Button btnSave, btnCancel;

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
        // 创建一个添加快捷工具的Item
        addToolsBean = new ToolsBean(-1, "add", R.mipmap.ic_add_to_list_nor);

        refreshShortcutTools();

    }

    private void initView() {
        
        try {
            rvAllTools = myView.findViewById(R.id.rv_all_tools_item);
            rvShortcutTools = myView.findViewById(R.id.rv_shortcut_tools_item);
            llSaveView = myView.findViewById(R.id.ll_tools_bottom_view);
            btnCancel = myView.findViewById(R.id.btn_cancel_save_tools);
            btnSave = myView.findViewById(R.id.btn_save_tools);


            allToolsItemListAdapter = new AllToolsItemListAdapter(mContext, allToolsBeanList);
            allToolsItemListAdapter.setAllToolsItemListAdapterListener(this);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext,MAX_SHORTCUT_TOOLS_NUM);
            rvAllTools.setLayoutManager(gridLayoutManager);
            rvAllTools.setAdapter(allToolsItemListAdapter);

            shortcutToolsItemListAdapter = new ShortcutToolsItemListAdapter(mContext, shortcutToolsBeanList);
            shortcutToolsItemListAdapter.setShortcutToolsItemListAdapterListener(this);
            // 这里的GridLayoutManager不能复用上面的，必须新建一个
            GridLayoutManager gridLayoutManager2 = new GridLayoutManager(mContext, MAX_SHORTCUT_TOOLS_NUM);
            rvShortcutTools.setLayoutManager(gridLayoutManager2);
            rvShortcutTools.setAdapter(shortcutToolsItemListAdapter);
            setShortcutToolsBeanList(shortcutToolsBeanList);

            setEditMode(false);
            btnSave.setOnClickListener(mListener);
            btnCancel.setOnClickListener(mListener);

        } catch (Exception exception){
            DebugLog.debug(exception.getMessage());
        }



    }

    /**
     *  设置快捷工具的Adapter数据
     *  @author wm
     *  @createTime 2023/9/26 15:00
     * @param list: 快捷工具列表，传进来的列表不带有“添加Item”
     */
    private void setShortcutToolsBeanList(List<ToolsBean> list){
        if (list.size() < MAX_SHORTCUT_TOOLS_NUM) {
             // 快捷工具未满3个时，把添加Item加在最后
            list.add(addToolsBean);
        }
        shortcutToolsItemListAdapter.setToolsBeanList(list);
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

    @Override
    public void toolsAddIconOnClick(int toolsId) {
        if (shortcutToolsBeanList.contains(addToolsBean)) {
            shortcutToolsBeanList.remove(addToolsBean);
        }
        if (shortcutToolsBeanList.size() < MAX_SHORTCUT_TOOLS_NUM){
            // 列表未满，继续添加
            shortcutToolsBeanList.add(allToolsBeanList.get(toolsId));
            setShortcutToolsBeanList(shortcutToolsBeanList);
        } else {
            Toast.makeText(mContext, "out of 3 shortcut tools!!!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClickItemByAll(int toolsId) {

    }

    @Override
    public void toolsDeleteIconOnClick(int toolsId) {

    }

    @Override
    public void onClickItemByShortcut(int toolsId) {
        if (toolsId == -1){
            setEditMode(true);
        }
    }

    @Override
    public void onIntoEditModeByShort() {
        setEditMode(true);
    }

    @Override
    public void onIntoEditModeByAll(){
        setEditMode(true);
    }

    private final View.OnClickListener mListener = view -> {
        if (view == btnSave) {
            try {
                setEditMode(false);
                // 保存快捷工具的List到SharedPreferences中
                List<Integer> saveList = new ArrayList<>();
                for (int i = 0; i < shortcutToolsBeanList.size() ; i++){
                    if (shortcutToolsBeanList.get(i).getToolsId() == -1){
                        // 跳过添加Item
                        continue;
                    }
                    saveList.add(shortcutToolsBeanList.get(i).getToolsId());
                }
                SharedPreferencesUtil.putListData(Constant.SHORTCUT_TOOLS_LIST, saveList);
            } catch (Exception exception) {
                DebugLog.debug(exception.getMessage());
            }
        } else if (view == btnCancel){
            setEditMode(false);
            refreshShortcutTools();
            setShortcutToolsBeanList(shortcutToolsBeanList);
        }
    };

    /**
     *  从SharedPreferencesU中获取快捷工具的集合，进入Fragment和退出编辑时需要调用
     *  @author wm
     *  @createTime 2023/9/26 14:56
     */
    private void refreshShortcutTools(){
        List<Integer> saveList  = SharedPreferencesUtil.getListData(Constant.SHORTCUT_TOOLS_LIST, Integer.class);
        shortcutToolsBeanList.clear();
        for (int i = 0 ; i < saveList.size(); i++){
            shortcutToolsBeanList.add(allToolsBeanList.get(saveList.get(i)));
        }
    }

    /**
     *  设置编辑模式
     *  @author wm
     *  @createTime 2023/9/26 14:56
     * @param state: true-开启编辑模式； false-关闭
     */
    private void setEditMode(boolean state){
        if (state){
            llSaveView.setVisibility(View.VISIBLE);
            allToolsItemListAdapter.setEditMode(true);
            shortcutToolsItemListAdapter.setEditMode(true);
        } else {
            llSaveView.setVisibility(View.GONE);
            allToolsItemListAdapter.setEditMode(false);
            shortcutToolsItemListAdapter.setEditMode(false);
        }
    }
}