package com.example.mediaplayproject.fragment.main;

import static com.example.mediaplayproject.base.BasicApplication.getApplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.AddToMusicListAdapter;
import com.example.mediaplayproject.adapter.CustomerMusicListAdapter;
import com.example.mediaplayproject.adapter.musiclist.MainListAdapter;
import com.example.mediaplayproject.adapter.tools.ShortcutToolsItemListAdapter;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.bean.MusicListBean;
import com.example.mediaplayproject.bean.ToolsBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author wm
 */
public class PersonalPageFragment extends Fragment implements CustomerMusicListAdapter.OnImageViewClickListener,
        MainListAdapter.MainListAdapterOnClickListener, ShortcutToolsItemListAdapter.ShortcutToolsItemListAdapterListener {

    private final Context mContext;
    private View myView;
    private List<MediaFileBean> defaultList = new ArrayList<>();
    private List<MediaFileBean> favoriteList = new ArrayList<>();
    private List<MediaFileBean> historyList = new ArrayList<>();
    private List<MusicListBean> customerLists = new ArrayList<>();
    private ListView lvCustomer;
    private CustomerMusicListAdapter customerMusicListAdapter;
    private ImageView ivDefaultPlaying, ivFavoritePlaying, ivHistoryPlaying, ivAddList, ivMainListViewBack,
            ivIntoSelectMode, ivAddToListBack, ivSelectAll, ivAddToList, ivDeleteSelectMusic;
    private String currentPlayingListName = Constant.LIST_MODE_DEFAULT_NAME;
    private String itemClickListName;

    private LinearLayout llDefaultList,llFavoriteList, llHistoryList;
    private MainListAdapter mainListAdapter;
    private RecyclerView rvMainList;
    private ListView lvAddToList;
    private LinearLayout llSelectAll, llAddToList, llDeleteSelectMusic;
    private TextView tvMainViewListName,tvSelectAll;
    private RelativeLayout mFloatLayout, mAddToListFloatLayouts;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams, overlayParams;
    boolean isShowList = false;
    private AddToMusicListAdapter addToMusicListAdapter;
    private Set<Long> deleteMusicHelperSet = new HashSet<>();
    private int mPosition = 0;

    private ShortcutToolsItemListAdapter.ShortcutToolsItemListAdapterListener shortcutToolsListener;
    private List<ToolsBean> allToolsBeanList = new ArrayList<>();
    private List<ToolsBean> shortcutToolsBeanList = new ArrayList<>();
    private ShortcutToolsItemListAdapter shortcutToolsItemListAdapter;
    private ToolsBean addToolsBean;

    @SuppressLint("StaticFieldLeak")
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_personal_page, container, false);
        initMusicSource();
        createFloatView();
        initData();
        myView.setOnTouchListener((v, event) -> {
            ToolsUtils.getInstance().hideKeyboard(myView);
            return false;
        });
        return myView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复页面都需要初始化，因为在MainActivity中是Fragment显示时才修改，若在后台则无法刷新
        refreshCustomerList();

        // 因为小工具更改处于不同页面，更改完了再进入本页面时初始化数据即可，不需要动态监听数据的变化
        refreshShortcutTools();
        setShortcutToolsBeanList();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWindowManager != null && mAddToListFloatLayouts.isAttachedToWindow()) {
            mWindowManager.removeView(mAddToListFloatLayouts);
        }
        if (mWindowManager != null && mFloatLayout.isAttachedToWindow()) {
            // 在MainActivity失去焦点时，悬浮窗关闭
            // 这里需要判断mFloatLayout正在显示才执行移除，否则会报错
            mWindowManager.removeView(mFloatLayout);
        }
    }

    /**
     *  从DataRefreshService中获取音乐列表等信息
     *  @author wm
     *  @createTime 2023/8/24 19:27
     */
    private void initMusicSource() {
        mPosition = DataRefreshService.getLastPosition();
        defaultList = DataRefreshService.getDefaultList();
        favoriteList = DataRefreshService.getFavoriteList();
        historyList = DataRefreshService.getHistoryList();
        customerLists = DataRefreshService.getCustomerList();
        currentPlayingListName = DataRefreshService.getLastPlayListName();
    }

    /**
     *  创建悬浮窗
     *  @author wm
     *  @createTime 2023/9/3 11:48
     */
    private void createFloatView() {
        // 创建音乐列表悬浮窗视图
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

        // 创建添加歌曲到列表的悬浮窗视图
        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // 不获取焦点，不影响后面的事件
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                // 设置为透明显示
                PixelFormat.TRANSLUCENT);

        initFloatView();
        initAddToListFloatView();
    }

    /**
     *  初始化音乐列表悬浮窗中的视图、初始化Fragment等
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
        mainListAdapter.setMainListAdapterOnClickListener(this);
        rvMainList = mFloatLayout.findViewById(R.id.rv_musicList);
        ivMainListViewBack = mFloatLayout.findViewById(R.id.iv_main_list_view_back);
        tvMainViewListName = mFloatLayout.findViewById(R.id.tv_main_view_list_name);
        ivIntoSelectMode = mFloatLayout.findViewById(R.id.iv_into_select_mode);
        llSelectAll = mFloatLayout.findViewById(R.id.ll_all_select);
        llAddToList = mFloatLayout.findViewById(R.id.ll_add_to_list);
        llDeleteSelectMusic = mFloatLayout.findViewById(R.id.ll_delete_select_music);
        tvSelectAll = mFloatLayout.findViewById(R.id.tv_select_all);
        ivSelectAll = mFloatLayout.findViewById(R.id.iv_all_select);
        ivAddToList = mFloatLayout.findViewById(R.id.iv_add_to_list);
        ivDeleteSelectMusic = mFloatLayout.findViewById(R.id.iv_delete_select_music);
        ivMainListViewBack.setOnClickListener(mListener);
        ivIntoSelectMode.setOnClickListener(mListener);
        llSelectAll.setOnClickListener(mListener);
        llAddToList.setOnClickListener(mListener);
        llDeleteSelectMusic.setOnClickListener(mListener);

        rvMainList.setAdapter(mainListAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        rvMainList.setLayoutManager(linearLayoutManager);

    }

    /**
     *  初始化添加歌曲到列表的悬浮窗视图
     *  @author wm
     *  @createTime 2023/9/8 17:19
     */
    @SuppressLint("InflateParams")
    private void initAddToListFloatView() {

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mAddToListFloatLayouts = (RelativeLayout) inflater.inflate(R.layout.layout_add_to_list_view, null);

        addToMusicListAdapter = new AddToMusicListAdapter(mContext,customerLists);
        lvAddToList = mAddToListFloatLayouts.findViewById(R.id.lv_add_musicList);
        ivAddToListBack = mAddToListFloatLayouts.findViewById(R.id.iv_add_to_list_back);
        ivAddToListBack.setOnClickListener(mListener);


        lvAddToList.setAdapter(addToMusicListAdapter);
        lvAddToList.setOnItemClickListener((parent, view, position, id) -> {
            try {
                List<Long> insertList = getSelectMusicIdList();
                if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(customerLists.get(position).getListName())){
                    DataRefreshService.addMultipleFavoriteMusic(insertList);
                } else {
                    DataRefreshService.insertCustomerMusic(customerLists.get(position).getListName(),insertList);
                }
                Toast.makeText(mContext,R.string.add_success,Toast.LENGTH_SHORT).show();
            } catch (Exception exception){
                Toast.makeText(mContext,R.string.add_failed,Toast.LENGTH_SHORT).show();
                DebugLog.debug("error " + exception.getMessage());
            }
            // 刷新列表的歌曲数量
            customerMusicListAdapter.notifyDataSetChanged();
            if (mWindowManager != null && mAddToListFloatLayouts.isAttachedToWindow()) {
                mWindowManager.removeView(mAddToListFloatLayouts);
            }
            setMainListSelectionState(false);
        });

    }

    /**
     * 显示悬浮窗
     * @author wm
     * @createTime 2023/9/2 14:18
     */
    @SuppressLint("NotifyDataSetChanged")
    private void showFloatView(List<MediaFileBean> musicList, String listName) {
        mainListAdapter.changeList(musicList,listName,currentPlayingListName);
        if(listName.equalsIgnoreCase(currentPlayingListName)){
            // 打开的列表是当前播放才设置高亮和定位
            mainListAdapter.setSelectPosition(mPosition);
            // 瞬间定位到子项
            rvMainList.scrollToPosition(mPosition);
            // 平滑地定位到子项 rvMainList.smoothScrollToPosition(mPosition);
        }
        setMainListSelectionState(false);
        if (mFloatLayout.isAttachedToWindow()){
            mWindowManager.removeView(mFloatLayout);
        }
        mWindowManager.addView(mFloatLayout, wmParams);
        mainListAdapter.notifyDataSetChanged();
        setListName(listName);
        tvSelectAll.setText(R.string.select_all);
        isShowList = true;
    }

    /**
     *  动态显示主页展开的列表名，
     *  Constant中定义名称常量不能兼容中英文两种翻译，所以固定的几个列表需要动态设定列表名称
     *  @author wm
     *  @createTime 2023/9/24 13:02
     * @param listName:
     */
    private void setListName(String listName){
        if (Constant.LIST_MODE_DEFAULT_NAME.equalsIgnoreCase(listName)){
            tvMainViewListName.setText(R.string.default_list_name);
        } else if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(listName)){
            tvMainViewListName.setText(R.string.favorite_list_name);
        } else if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(listName)){
            tvMainViewListName.setText(R.string.history_list_name);
        } else {
            tvMainViewListName.setText(listName);
        }
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

    /**
     *  进入多选状态时的回调方法
     *  MainListAdapter的回调方法
     *  @author wm
     *  @createTime 2023/9/8 22:23
     */
    @Override
    public void onLongClickToCheckBoxState() {
        // 进入多选状态，多选框可用
        llSelectAll.setEnabled(true);
        ivSelectAll.setImageResource(R.mipmap.ic_checkbox_pre);
    }

    /**
     *  根据选中列表是否为空来更新“添加” 、“删除”按钮的状态
     *  MainListAdapter的回调方法
     *  @author wm
     *  @createTime 2023/9/8 22:22
     * @param isSetEmpty: 列表是否为空， true-空；false-非空
     */
    @Override
    public void onSelectedItem(boolean isSetEmpty) {
        llAddToList.setEnabled(!isSetEmpty);
        llDeleteSelectMusic.setEnabled(!isSetEmpty);
        ivAddToList.setImageResource(isSetEmpty ? R.mipmap.ic_add_to_list_nor : R.mipmap.ic_add_to_list_pre);
        ivDeleteSelectMusic.setImageResource(isSetEmpty ? R.mipmap.ic_delete_select_nor : R.mipmap.ic_delete_select_pre);
        String listName = mainListAdapter.getListName();
        if (Constant.LIST_MODE_DEFAULT_NAME.equalsIgnoreCase(listName)){
            // 默认列表不支持删除操作
            llDeleteSelectMusic.setEnabled(false);
            ivDeleteSelectMusic.setImageResource(R.mipmap.ic_delete_select_nor);
        }
    }

    private void initData() {
        lvCustomer = myView.findViewById(R.id.lv_customer_list);
        ivDefaultPlaying = myView.findViewById(R.id.iv_default_list_playing);
        ivFavoritePlaying = myView.findViewById(R.id.iv_favorite_list_playing);
        ivHistoryPlaying = myView.findViewById(R.id.iv_history_list_playing);
        customerMusicListAdapter = new CustomerMusicListAdapter(mContext,customerLists,currentPlayingListName);
        customerMusicListAdapter.setOnImageViewClickListener(this);
        lvCustomer.setAdapter(customerMusicListAdapter);
        registerForContextMenu(lvCustomer);

        ivAddList = myView.findViewById(R.id.iv_add_list);
        ivAddList.setOnClickListener(mListener);

        lvCustomer.setOnItemClickListener((parent, view, position, id) -> {
            List<MediaFileBean> list = customerLists.get(position).getMusicList();
            showFloatView(list, customerLists.get(position).getListName());
        });

        llDefaultList = myView.findViewById(R.id.ll_default_list);
        llFavoriteList = myView.findViewById(R.id.ll_favorite_list);
        llHistoryList = myView.findViewById(R.id.ll_history_list);
        llDefaultList.setOnClickListener(mListener);
        llFavoriteList.setOnClickListener(mListener);
        llHistoryList.setOnClickListener(mListener);


        initItemBean();
        RecyclerView rvShortcutTools = myView.findViewById(R.id.rv_shortcut_tools_in_personal);
        shortcutToolsItemListAdapter = new ShortcutToolsItemListAdapter(mContext, shortcutToolsBeanList);
        shortcutToolsItemListAdapter.setShortcutToolsItemListAdapterListener(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext, Constant.MAX_SHORTCUT_TOOLS_NUM);
        rvShortcutTools.setLayoutManager(gridLayoutManager);
        rvShortcutTools.setAdapter(shortcutToolsItemListAdapter);
        setShortcutToolsBeanList();
    }



    /**
     *  显示创建列表的弹框
     *  @author wm
     *  @createTime 2023/9/4 15:57
     */
    private void showCreateListAliasDialog() {
        EditText inputText = new EditText(mContext);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.create_custom_list);
        builder.setMessage(R.string.input_custom_list_name);
        builder.setIcon(R.mipmap.ic_customer_pre);
        builder.setView(inputText);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        //设置正面按钮
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String inputListName = inputText.getText().toString().trim();
            if (!"".equals(inputListName)) {
                if (DataRefreshService.getMusicListByName(inputListName) != null) {
                    Toast.makeText(mContext, R.string.create_list_repeat, Toast.LENGTH_LONG).show();
                } else if (inputListName.length() > Constant.MAX_LENGTH_OF_LIST_NAME) {
                    String listNameLimitTip = this.getString(R.string.list_name_limit) + Constant.MAX_LENGTH_OF_LIST_NAME +")！";
                    Toast.makeText(mContext, listNameLimitTip, Toast.LENGTH_LONG).show();
                } else {
                    DataRefreshService.createNewCustomerMusicList(inputListName);
                    Toast.makeText(mContext, R.string.create_success, Toast.LENGTH_LONG).show();
                }
            }
            dialog.dismiss();
        });

        //设置反面按钮
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        //创建AlertDialog对象
        AlertDialog dialog = builder.create();

        //显示对话框
        dialog.show();
    }

    /**
     *  对列表右边“更多”按钮的点击监听回调
     *  CustomerMusicListAdapter的回调方法
     *  @author wm
     *  @createTime 2023/9/8 22:28
     * @param view:
     * @param listName:
     */
    @Override
    public void onSettingImageViewClick(View view, String listName) {
        itemClickListName = listName;
        view.showContextMenu();
    }

    /**
     *  用来帮助创建“更多”按钮唤出Menu菜单
     *  @author wm
     *  @createTime 2023/9/8 22:33
     * @param menu:
     * @param v:
     * @param menuInfo:
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        new MenuInflater(mContext).inflate(R.menu.customer_list_function_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    /**
     *  “更多”按钮被点击时触发该方法
     *  @author wm
     *  @createTime 2023/9/4 11:40
     *  @param item: 菜单子项
     *  @return : boolean
     */
    @SuppressLint("NonConstantResourceId")
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
                showDeleteListAliasDialog();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     *  显示删除列表的弹框
     *  @author wm
     *  @createTime 2023/9/4 15:57
     */
    private void showDeleteListAliasDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        /// 若设置的字符是需要几个字符拼接起来的，不能直接使用R.string.xxx，需要使用this.getString先获取字符
        /// 且不能直接在setTitle中拼接，需要先拼接到String中再使用,如下面的语句不会正常输出
        /// builder.setTitle(this.getString(R.string.delete_custom_list) + " " + itemClickListName +);
        String title = this.getString(R.string.delete_custom_list) + " " +
                itemClickListName + " " + this.getString(R.string.list);
        builder.setTitle(title);
        if (currentPlayingListName.equalsIgnoreCase(itemClickListName)){
            builder.setMessage(R.string.delete_music_tip);
        } else {
            builder.setMessage("");
        }

        builder.setIcon(R.mipmap.ic_delete_select_pre);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        // 设置正面按钮
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            try {
                // 删除音乐列表
                DataRefreshService.deleteCustomerMusicList(itemClickListName);
                dialog.dismiss();
                Toast.makeText(mContext, R.string.delete_success,Toast.LENGTH_SHORT).show();
            } catch (Exception exception){
                Toast.makeText(mContext, R.string.delete_failed,Toast.LENGTH_SHORT).show();
                DebugLog.debug("error " + exception.getMessage());
            }
        });

        // 设置反面按钮
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        // 创建AlertDialog对象
        AlertDialog dialog = builder.create();
        // 显示对话框
        dialog.show();
    }


    /**
     *  显示删除音乐的弹框
     *  @author wm
     *  @createTime 2023/9/4 15:57
     */
    private void showDeleteMusicDialog() {
        String listName = mainListAdapter.getListName();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        List<Long> deleteList = getSelectMusicIdList();
        builder.setTitle(R.string.delete_music);
        long lastPlayId = DataRefreshService.getLastMusicId();
        boolean includePlayingMusic = currentPlayingListName.equalsIgnoreCase(listName) && deleteMusicHelperSet.contains(lastPlayId);
        if (includePlayingMusic){
            builder.setMessage(R.string.delete_music_tip);
        } else {
            builder.setMessage("");
        }

        builder.setIcon(R.mipmap.ic_launcher);
        // 点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        // 设置正面按钮
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            try {
                DataRefreshService.deleteMultipleMusic(listName,deleteList);
                Toast.makeText(mContext, R.string.delete_success,Toast.LENGTH_SHORT).show();
            } catch (Exception exception){
                Toast.makeText(mContext, R.string.delete_failed,Toast.LENGTH_SHORT).show();
                DebugLog.debug("error " + exception.getMessage());
            }
            // 需要刷新页面
            customerMusicListAdapter.notifyDataSetChanged();
            setMainListSelectionState(false);
            mWindowManager.removeView(mFloatLayout);
            dialog.dismiss();
        });

        // 设置反面按钮
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        // 创建AlertDialog对象
        AlertDialog dialog = builder.create();

        // 加上这个属性，让Dialog显示于悬浮窗页面之上
        Window window = dialog.getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        // 显示对话框
        dialog.show();
    }

    /**
     *  获取选中音乐的Id集合List<Long>
     *  @author wm
     *  @createTime 2023/9/9 10:26
     * @return : java.util.List<java.lang.Long>
     */
    private List<Long> getSelectMusicIdList(){
        try{
            deleteMusicHelperSet.clear();
            String listName = mainListAdapter.getListName();
            Set<Integer> selectedItems = mainListAdapter.getSelectedItems();
            List<MediaFileBean> list = DataRefreshService.getMusicListByName(listName);
            List<Long> insertList = new ArrayList<>();
            for (Integer selectedItem : selectedItems) {
                int musicPosition = selectedItem;
                insertList.add(list.get(musicPosition).getId());
                deleteMusicHelperSet.add(list.get(musicPosition).getId());
            }
            return insertList;
        }  catch (Exception exception){
            DebugLog.debug("error " + exception.getMessage());
            return null;
        }
    }

    /**
     *  更新自定义列表的状态
     *  @author wm
     *  @createTime 2023/9/3 23:02
     */
    public void refreshCustomerList() {
        initMusicSource();
        if (customerMusicListAdapter != null){
            customerMusicListAdapter.setCurrentPlayingListName(currentPlayingListName);
            customerMusicListAdapter.changeCustomerList(customerLists);
        }
        // 刷新默认列表和收藏列表的播放状态图标
        if (Constant.LIST_MODE_DEFAULT_NAME.equalsIgnoreCase(currentPlayingListName)){
            ivDefaultPlaying.setVisibility(View.VISIBLE);
            ivFavoritePlaying.setVisibility(View.GONE);
            ivHistoryPlaying.setVisibility(View.GONE);
        } else if(Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(currentPlayingListName)){
            ivDefaultPlaying.setVisibility(View.GONE);
            ivFavoritePlaying.setVisibility(View.VISIBLE);
            ivHistoryPlaying.setVisibility(View.GONE);
        } else if(Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(currentPlayingListName)){
            ivDefaultPlaying.setVisibility(View.GONE);
            ivFavoritePlaying.setVisibility(View.GONE);
            ivHistoryPlaying.setVisibility(View.VISIBLE);
        } else {
            ivDefaultPlaying.setVisibility(View.GONE);
            ivFavoritePlaying.setVisibility(View.GONE);
            ivHistoryPlaying.setVisibility(View.GONE);
        }
        // 刷新展开列表的高亮状态，并定位
        mainListAdapter.setSelectPosition(mPosition);
        rvMainList.scrollToPosition(mPosition);
    }

    /**
     *  切换多选状态时的UI处理
     *  @author wm
     *  @createTime 2023/9/10 15:42
     * @param state: true-多选模式； false-退出多选模式
     */
    @SuppressLint("NotifyDataSetChanged")
    private void setMainListSelectionState(boolean state){
        if (state){
            mainListAdapter.setSelectionState(true);
            llSelectAll.setEnabled(true);
            ivSelectAll.setImageResource(R.mipmap.ic_checkbox_pre);
        } else {
            mainListAdapter.setSelectionState(false);
            llSelectAll.setEnabled(false);
            llAddToList.setEnabled(false);
            llDeleteSelectMusic.setEnabled(false);
            ivSelectAll.setImageResource(R.mipmap.ic_checkbox_nor);
            ivAddToList.setImageResource(R.mipmap.ic_add_to_list_nor);
            ivDeleteSelectMusic.setImageResource(R.mipmap.ic_delete_select_nor);
            mainListAdapter.notifyDataSetChanged();
        }
    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivAddList) {
            showCreateListAliasDialog();
        } else if (view == llDefaultList){
            showFloatView(defaultList, Constant.LIST_MODE_DEFAULT_NAME);
        } else if (view == llFavoriteList){
            showFloatView(favoriteList, Constant.LIST_MODE_FAVORITE_NAME);
        }  else if (view == llHistoryList){
            showFloatView(historyList, Constant.LIST_MODE_HISTORY_NAME);
        } else if (view == ivMainListViewBack){
            boolean isSelectMode = mainListAdapter.isSelectionMode();
            if (isSelectMode){
                // 退出多选状态
                if (mainListAdapter != null){
                    setMainListSelectionState(false);
                }
            } else {
                // 关闭悬浮窗
                if (mWindowManager != null && mFloatLayout.isAttachedToWindow()) {
                    mWindowManager.removeView(mFloatLayout);
                }
            }
        } else if (view == ivIntoSelectMode){
            // 进入多选模式
            setMainListSelectionState(true);
        }  else if (view == llSelectAll){
            boolean isSelectedAll = mainListAdapter.isSelectionAll();
            mainListAdapter.selectAllItem(!isSelectedAll);
            tvSelectAll.setText(isSelectedAll ? R.string.select_all : R.string.cancel_select_all);
        } else if(view == llAddToList){
            // 在打开添加页面之前，需要刷新一下自定义创建列表的数据
            initMusicSource();
            addToMusicListAdapter.changeCustomerList(customerLists);
            mWindowManager.addView(mAddToListFloatLayouts, overlayParams);
        } else if(view == llDeleteSelectMusic){
            showDeleteMusicDialog();
        } else if (view == ivAddToListBack){
            if (mWindowManager != null && mAddToListFloatLayouts.isAttachedToWindow()) {
                mWindowManager.removeView(mAddToListFloatLayouts);
            }
        }
    };

    /**
     *  初始化toolsItemList数据
     *  @author wm
     *  @createTime 2023/9/26 17:31
     */
    private void initItemBean() {
        allToolsBeanList = ToolsUtils.getInstance().getAllToolsList(mContext);
        // 创建一个添加快捷工具的Item
        addToolsBean = new ToolsBean(-1, "add", R.mipmap.ic_add_to_list_nor);
        refreshShortcutTools();
    }

    /**
     *  从SharedPreferencesU中获取快捷工具的集合，进入Fragment和小工具刷新时需要调用
     *  @author wm
     *  @createTime 2023/9/26 14:56
     */
    private void refreshShortcutTools(){
        shortcutToolsBeanList = ToolsUtils.getInstance().getShortcutToolsList(mContext, allToolsBeanList);
    }

    /**
     *  设置快捷工具的Adapter数据
     *  @author wm
     *  @createTime 2023/9/26 15:00
     */
    private void setShortcutToolsBeanList(){
        if (!shortcutToolsBeanList.contains(addToolsBean) && shortcutToolsBeanList.size() < Constant.MAX_SHORTCUT_TOOLS_NUM) {
            // 快捷工具未满3个，且未包含“添加按钮”，把添加Item加在最后
            shortcutToolsBeanList.add(addToolsBean);
        }
        shortcutToolsItemListAdapter.setToolsBeanList(shortcutToolsBeanList);
    }

    @Override
    public void toolsDeleteIconOnClick(int toolsId) {

    }

    @Override
    public void onClickItemByShortcut(int toolsId) {
        ToolsUtils.getInstance().startToolsFragmentById(mContext,toolsId);
    }

    @Override
    public void onIntoEditModeByShort() {

    }
}