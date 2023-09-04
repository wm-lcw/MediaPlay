package com.example.mediaplayproject.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.CustomerMusicListAdapter;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.bean.MusicListBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.ArrayList;
import java.util.List;


/**
 * @author wm
 */
public class PersonalPageFragment extends Fragment implements CustomerMusicListAdapter.OnImageViewClickListener {

    private Context mContext;
    private View myView;
    private List<MediaFileBean> defaultList = new ArrayList<>();
    private List<MediaFileBean> favoriteList = new ArrayList<>();
    private List<MusicListBean> customerLists = new ArrayList<>();
    private ListView lvCustomer;
    private CustomerMusicListAdapter customerMusicListAdapter;
    private ImageView ivAddList;
    private String currentPlayingListName = Constant.LIST_MODE_DEFAULT_NAME;

    private static PersonalPageFragment instance;

    public PersonalPageFragment(Context context) {
        mContext = context;
    }

    public static PersonalPageFragment getInstance(Context context) {
        if (instance == null) {
            instance = new PersonalPageFragment(context);
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
        myView = inflater.inflate(R.layout.fragment_personal_page, container, false);
        initMusicSource();
        initData();
        return myView;
    }

    private void initData() {
        lvCustomer = myView.findViewById(R.id.lv_customer_list);
        DebugLog.debug("---" + customerLists);
        customerMusicListAdapter = new CustomerMusicListAdapter(mContext,customerLists,currentPlayingListName);
        customerMusicListAdapter.setOnImageViewClickListener(this);
        lvCustomer.setAdapter(customerMusicListAdapter);
        registerForContextMenu(lvCustomer);

        ivAddList = myView.findViewById(R.id.iv_add_list);
        ivAddList.setOnClickListener(mListener);

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        initMusicSource();
        if (lvCustomer!= null && customerMusicListAdapter!=null){
            DebugLog.debug("");
        }
    }

    /**
     *  从DataRefreshService中获取音乐列表等信息
     *  @author wm
     *  @createTime 2023/8/24 19:27
     */
    private void initMusicSource() {
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        customerLists = DataRefreshService.getCustomerList();
        currentPlayingListName = DataRefreshService.getLastPlayListName();
    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivAddList) {
            showCreateListAliasDialog();
        }
    };

    /**
     *  显示创建列表的弹框
     *  @author wm
     *  @createTime 2023/9/4 15:57
     */
    private void showCreateListAliasDialog() {
        EditText inputText = new EditText(mContext);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("创建列表");
        builder.setMessage("请输入列表名称：");
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setView(inputText);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        //设置正面按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            String inputListName = inputText.getText().toString().trim();
            if (!"".equals(inputListName)){
                DataRefreshService.createNewMusicList(inputListName);
            }
            dialog.dismiss();
        });

        //设置反面按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        //创建AlertDialog对象
        AlertDialog dialog = builder.create();

        //显示对话框
        dialog.show();
    }



    @Override
    public void onImageViewClick(View view) {
        view.showContextMenu();
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        new MenuInflater(mContext).inflate(R.menu.customer_list_function_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    /**
     *  上下文菜单被点击时触发该方法
     *  @author wm
     *  @createTime 2023/9/4 11:40
     *  @param item:
     *  @return : boolean
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                DebugLog.debug("share");
                break;
            case R.id.edit:
                DebugLog.debug("edit");
                break;
            case R.id.delete:
                DebugLog.debug("delete");
                break;
            default:
                break;
        }
        return true;
    }


    /**
     *  更新自定义列表的状态
     *  @author wm
     *  @createTime 2023/9/3 23:02
     */
    public void refreshCustomerList() {
        initMusicSource();
        if (customerMusicListAdapter != null){
            customerMusicListAdapter.changeCustomerList(customerLists);
        }
    }

    /**
     *  更新当前播放列表
     *  @author wm
     *  @createTime 2023/9/4 16:46
     * @param musicListName: 当前播放列表名称
     */
    public void refreshCurrentPlayingList(String musicListName) {
        this.currentPlayingListName = musicListName;
        customerMusicListAdapter.setCurrentPlayingListName(musicListName);
    }

}