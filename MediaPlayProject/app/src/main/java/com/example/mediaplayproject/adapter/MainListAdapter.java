package com.example.mediaplayproject.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.DebugLog;

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
    private boolean isSelectionMode = false, isSelectionAll = false;

    public MainListAdapter(Context context, List<MediaFileBean> musicList) {
        this.mContext = context;
        this.musicList = musicList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void changeList(List<MediaFileBean> newList){
        this.musicList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MainListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_music_list_item, parent, false);
        return new ViewHolder(view);
    }

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
            return false;
        });

        holder.musicName.setOnClickListener(v -> {
            if (isSelectionMode){
                // 多选模式下，点击歌曲名也能选中跟取消
                toggleSelection(position);
            } else {
                // 非多选模式下，通知Activity播放音乐
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
        });


        holder.musicCheckBox.setChecked(selectedItems.contains(position));
        holder.musicCheckBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
    }

    private void toggleSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
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

    public void setCheckoutState(boolean state){
        isSelectionMode = state;
    }

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
    }

    public boolean isSelectionAll(){
        return isSelectionAll;
    }

    /*
    * 首次加载时，哪部分展示出来了，就执行以下逻辑- 执行顺序：onCreateViewHolder--> new ViewHolder --> onBindViewHolder
    *
    * 全部加载完之后，哪部分展示，就只加载onBindViewHolder，因为已经创建过了，直接复用
    *
    * */
}
