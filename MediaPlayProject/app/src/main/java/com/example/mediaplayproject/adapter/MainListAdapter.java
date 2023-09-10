package com.example.mediaplayproject.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author wm
 * @Classname MainListAdapter
 * @Date 2023/9/4 23:11
 * @Created by wm
 */
public class MainListAdapter extends RecyclerView.Adapter<MainListAdapter.ViewHolder>{

    private List<MediaFileBean> musicList;
    private Context mContext;
    private Set<Integer> selectedItems = new HashSet<>();
    private boolean isSelectionMode = false, isSelectionAll = false, isSelectSetEmpty = true;
    private String listName = Constant.LIST_MODE_DEFAULT_NAME;
    private MainListAdapterOnClickListener mListener;
    private int defaultSelection = -1;
    private int text_selected_color;
    private ColorStateList colors;
    private String currentPlayingListName = Constant.LIST_MODE_DEFAULT_NAME;

    public MainListAdapter(Context context, List<MediaFileBean> musicList) {
        this.mContext = context;
        this.musicList = musicList;
        Resources resources = mContext.getResources();
        // 文字选中的颜色
        text_selected_color = resources.getColor(R.color.text_pressed);
        // 文字未选中状态的selector
        colors = mContext.getResources().getColorStateList(R.color.listview_text_color_selector);
        resources = null;
    }

    /**
     *  更改Adapter的数据来源
     *  @author wm
     *  @createTime 2023/9/8 22:40
     * @param newList: 新的音乐列表
     * @param listName: 新列表的列表名
     */
    @SuppressLint("NotifyDataSetChanged")
    public void changeList(List<MediaFileBean> newList, String listName, String currentPlayingListName){
        this.musicList = newList;
        this.listName = listName;
        this.currentPlayingListName = currentPlayingListName;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MainListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_music_list_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull MainListAdapter.ViewHolder holder, int position) {
        MediaFileBean mediaFileBean = musicList.get(position);
        holder.musicName.setText(mediaFileBean.getTitle());
        holder.musicName.setOnLongClickListener(v -> {
            // 长按切换选中状态
            isSelectionMode = true;
            selectedItems.clear();
            toggleSelection(position);
            notifyDataSetChanged();
            if (mListener != null){
                mListener.onLongClickToCheckBoxState();
            }
            return false;
        });

        holder.musicName.setOnClickListener(v -> {
            if (isSelectionMode){
                // 多选模式下，点击歌曲名也能选中跟取消
                toggleSelection(position);
            } else {
                // 非多选模式下，通知Activity播放音乐
                Intent intent = new Intent(Constant.CHANGE_MUSIC_ACTION);
                Bundle bundle = new Bundle();
                bundle.putInt("position", position);
                bundle.putString("musicListName", listName);
                intent.putExtras(bundle);
                mContext.sendBroadcast(intent);
                holder.musicName.setTextColor(text_selected_color);
            }
        });

        holder.musicCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked){
                selectedItems.add(position);
            } else {
                if (selectedItems.contains(position)) {
                    selectedItems.remove(position);
                }
            }
            if (checkSelectedItem()){
                mListener.onSelectedItem(isSelectSetEmpty);
            }
        });

        holder.musicCheckBox.setChecked(selectedItems.contains(position));
        holder.musicCheckBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        // 是当前播放列表、单选模式，且是当前播放歌曲，符合这三个条件才高亮
        if (currentPlayingListName.equalsIgnoreCase(listName) && !isSelectionMode && position == defaultSelection){
            holder.musicName.setTextColor(text_selected_color);
        } else {
            holder.musicName.setTextColor(colors);
        }
    }

    /**
     *  多选状态下，对歌曲名的点击事件做选中/取消选中处理
     *  @author wm
     *  @createTime 2023/9/8 22:50
     * @param position: 下标
     */
    private void toggleSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
        }
        if (checkSelectedItem()){
            mListener.onSelectedItem(isSelectSetEmpty);
        }
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView musicName;
        CheckBox musicCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            musicName = itemView.findViewById(R.id.tv_music_name);
            musicCheckBox = itemView.findViewById(R.id.cb_music);
            musicCheckBox.setVisibility(View.GONE);
        }
    }

    /**
     *  设置当前的状态（是否为多选状态）
     *  @author wm
     *  @createTime 2023/9/8 22:55
     * @param state: true-多选状态； false-点击播放状态
     */
    public void setSelectionState(boolean state){
        isSelectionMode = state;
        if (!state){
            selectedItems.clear();
            isSelectSetEmpty = true;
        }
        notifyDataSetChanged();
    }

    /**
     *  获取当前是否是多选状态
     *  @author wm
     *  @createTime 2023/9/8 22:56
     * @return : boolean： true-选中状态； false-非选中状态
     */
    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    /**
     *  设置全选
     *  @author wm
     *  @createTime 2023/9/8 23:00
     * @param isAllSelected: true-全选； false-取消全选
     */
    public void selectAllItem(boolean isAllSelected){
        isSelectionAll = isAllSelected;
        if (isSelectionAll){
            for (int i = 0; i < musicList.size(); i++) {
                selectedItems.add(i);
            }
        } else {
            selectedItems.clear();
        }
        notifyDataSetChanged();
        if (checkSelectedItem()){
            mListener.onSelectedItem(isSelectSetEmpty);
        }
    }

    /**
     *  获取当前是否是全选状态
     *  @author wm
     *  @createTime 2023/9/8 23:01
     * @return : boolean：true-是全选； false-非全选
     */
    public boolean isSelectionAll(){
        return isSelectionAll;
    }

    /**
     *  获取选中的集合set
     *  @author wm
     *  @createTime 2023/9/8 23:0
     * @return : java.util.Set<java.lang.Integer>
     */
    public Set<Integer> getSelectedItems() {
        return selectedItems;
    }

    /**
     *  获取当前展示列表的列表名
     *  @author wm
     *  @createTime 2023/9/8 23:02
     * @return : java.lang.String
     */
    public String getListName() {
        return listName;
    }

    /**
     *  检测Set里面元素的数量是否在空与非空之间转化
     *  @author wm
     *  @createTime 2023/9/8 21:24
     * @return : boolean: true-有更改，需要更新“添加到”的按钮状态； false-无更改
     */
    public boolean checkSelectedItem(){
        boolean tempFlag = selectedItems.size() <= 0;
        if (isSelectSetEmpty && tempFlag) {
            return false;
        } else if (!isSelectSetEmpty && !tempFlag) {
            return false;
        }
        isSelectSetEmpty = tempFlag;
        return true;
    }

    public interface MainListAdapterOnClickListener {

        /**
         *  长按更换为多选状态的回调方法
         *  @author wm
         *  @createTime 2023/9/8 20:55
         */
        void onLongClickToCheckBoxState();

        /**
         *  是否有选中，用来通知更新“添加到”按钮的状态
         *  @author wm
         *  @createTime 2023/9/8 21:56
         * @param isSetEmpty: 选中列表是否为空
         */
        void onSelectedItem(boolean isSetEmpty);
    }

    public void setMainListAdapterOnClickListener(MainListAdapterOnClickListener listener){
        mListener = listener;
    }

    /**
     *  选中高亮效果
     *  @author wm
     *  @createTime 2023/9/3 18:06
     * @param position: 需要高亮的歌曲下标
     */
    public void setSelectPosition(int position) {
        if (position == -1) {
            //若传进来的值是-1，则代表要取消播放歌曲高亮效果
            defaultSelection = position;
        }
        if (!(position < 0 || position > musicList.size())) {
            defaultSelection = position;
        }
        notifyDataSetChanged();
    }

    /*
    * 首次加载时，哪部分展示出来了，就执行以下逻辑- 执行顺序：onCreateViewHolder--> new ViewHolder --> onBindViewHolder
    *
    * 全部加载完之后，哪部分展示，就只加载onBindViewHolder，因为已经创建过了，直接复用
    *
    * */
}
