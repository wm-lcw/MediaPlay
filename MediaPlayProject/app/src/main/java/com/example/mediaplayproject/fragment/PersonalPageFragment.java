package com.example.mediaplayproject.fragment;

import static com.example.mediaplayproject.base.BasicApplication.getApplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Message;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.CustomerMusicListAdapter;
import com.example.mediaplayproject.adapter.ListViewPagerAdapter;
import com.example.mediaplayproject.adapter.MainListAdapter;
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
    private String itemClickListName;

    private LinearLayout llDefaultList,llFavoriteList;

    private MainListAdapter mainListAdapter;
    private RecyclerView rvMainList;
    private LinearLayout llSelectAll, llAddToList, llDeleteSelectMusic;
    private TextView tvSelectAll;

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

        createFloatView();
        customerMusicListAdapter = new CustomerMusicListAdapter(mContext,customerLists,currentPlayingListName);
        customerMusicListAdapter.setOnImageViewClickListener(this);
        lvCustomer.setAdapter(customerMusicListAdapter);
        registerForContextMenu(lvCustomer);

        ivAddList = myView.findViewById(R.id.iv_add_list);
        ivAddList.setOnClickListener(mListener);

        lvCustomer.setOnItemClickListener((parent, view, position, id) -> {
            List<MediaFileBean> list = customerLists.get(position).getMusicList();
            showFloatView(list);
        });

        llDefaultList = myView.findViewById(R.id.ll_default_list);
        llFavoriteList = myView.findViewById(R.id.ll_favorite_list);
        llDefaultList.setOnClickListener(mListener);
        llFavoriteList.setOnClickListener(mListener);


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
        } else if (view == llDefaultList){
            showFloatView(defaultList);
        } else if (view == llFavoriteList){
            showFloatView(favoriteList);
        } else if (view == llSelectAll){
            boolean isSelectedAll = mainListAdapter.isSelectionAll();
            mainListAdapter.selectAllItem(!isSelectedAll);
            tvSelectAll.setText(isSelectedAll ? "全选" : "取消全选");

        } else if(view == llAddToList){
//            DebugLog.debug("-----");
//            WindowManager.LayoutParams overlayParams = new WindowManager.LayoutParams(
//                    WindowManager.LayoutParams.WRAP_CONTENT,
//                    WindowManager.LayoutParams.WRAP_CONTENT,
//                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                    // 不获取焦点，不影响后面的事件
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                    // 设置为透明显示
//                    PixelFormat.TRANSLUCENT);
//            TextView overlayView = new TextView(mContext);
//            overlayView.setText("Overlay Textsdfkjhskdjfhkjsfg");
//            overlayView.setTextColor(Color.WHITE);
//            overlayView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    DebugLog.debug("sjdhgfjhsd");
//                }
//            });
//            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
//            windowManager.addView(overlayView, overlayParams);
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
    public void onSettingImageViewClick(View view, String listName) {
        itemClickListName = listName;
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
                break;
            case R.id.edit:
                break;
            case R.id.delete:
                showDeleteListAliasDialog();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     *  删除音乐列表
     *  @author wm
     *  @createTime 2023/9/4 17:54
     */
    private void toDeleteMusicList() {
        DataRefreshService.deleteMusicList(itemClickListName);
    }

    /**
     *  显示删除列表的弹框
     *  @author wm
     *  @createTime 2023/9/4 15:57
     */
    private void showDeleteListAliasDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("是否删除'" + itemClickListName +"'列表?");
        if (currentPlayingListName.equalsIgnoreCase(itemClickListName)){
            builder.setMessage("当前正在播放此列表，是否删除该列表？");
        } else {
            builder.setMessage("是否删除该列表？");
        }

        builder.setIcon(R.mipmap.ic_launcher);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        // 设置正面按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            toDeleteMusicList();
            dialog.dismiss();
        });

        // 设置反面按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        // 创建AlertDialog对象
        AlertDialog dialog = builder.create();
        // 显示对话框
        dialog.show();
    }



    /**
     *  更新自定义列表的状态
     *  @author wm
     *  @createTime 2023/9/3 23:02
     */
    public void refreshCustomerList() {
        DebugLog.debug("");
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

    private RelativeLayout mFloatLayout;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    boolean isShowList = false;

    /**
     *  创建悬浮窗
     *  @author wm
     *  @createTime 2023/9/3 11:48
     */
    private void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        // 获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        // 设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        // 设置背景为透明，否则滑动ListView会出现残影
        wmParams.format = PixelFormat.TRANSPARENT;
        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口,不设置这个flag的话，home页的划屏会有问题
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        // 调整悬浮窗显示的停靠位置为左侧顶部
        wmParams.gravity = Gravity.START | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        // 设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        initFloatView();
    }

    /**
     *  初始化悬浮窗中的视图、初始化Fragment等
     *  @author wm
     *  @createTime 2023/9/3 16:50
     */
    @SuppressLint("InflateParams")
    private void initFloatView() {
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.layout_main_list_view, null);
        setWindowOutTouch();
        mainListAdapter = new MainListAdapter(mContext,defaultList);
        rvMainList = mFloatLayout.findViewById(R.id.rv_musicList);
        llSelectAll = mFloatLayout.findViewById(R.id.ll_all_select);
        llAddToList = mFloatLayout.findViewById(R.id.ll_add_to_list);
        llDeleteSelectMusic = mFloatLayout.findViewById(R.id.ll_delete_select_music);
        tvSelectAll = mFloatLayout.findViewById(R.id.tv_select_all);
        llSelectAll.setOnClickListener(mListener);
        llAddToList.setOnClickListener(mListener);
        llDeleteSelectMusic.setOnClickListener(mListener);
        
        rvMainList.setAdapter(mainListAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        rvMainList.setLayoutManager(linearLayoutManager);

    }


    /**
     * 显示悬浮窗
     * @author wm
     * @createTime 2023/9/2 14:18
     */
    @SuppressLint("NotifyDataSetChanged")
    private void showFloatView(List<MediaFileBean> musicList) {
        mainListAdapter.changeList(musicList);
        mainListAdapter.setCheckoutState(false);
        mWindowManager.addView(mFloatLayout, wmParams);
        mainListAdapter.notifyDataSetChanged();
        mainListAdapter.setCheckoutState(false);
        tvSelectAll.setText("全选");
        isShowList = true;
    }

    /**
     *  点击窗口外部区域关闭列表窗口
     *  @author wm
     *  @createTime 2023/9/3 16:51
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setWindowOutTouch() {
        /* 点击窗口外部区域可消除
         将悬浮窗设置为全屏大小，外层有个透明背景，中间一部分视为内容区域,
         所以点击内容区域外部视为点击悬浮窗外部
         其中popupWindowView为全屏，listWindow为列表区域，触摸点没有落在列表区域，则隐藏列表*/
        final View popupWindowView = mFloatLayout.findViewById(R.id.rl_popup_window);
        final View listWindow = mFloatLayout.findViewById(R.id.rl_listWindow);
        popupWindowView.setOnTouchListener((v, event) -> {
            int x = (int) event.getX();
            int y = (int) event.getY();
            Rect rect = new Rect();
            listWindow.getGlobalVisibleRect(rect);
            if (!rect.contains(x, y) && isShowList) {
                mWindowManager.removeView(mFloatLayout);
                isShowList = false;
            }
            return true;
        });
    }


}