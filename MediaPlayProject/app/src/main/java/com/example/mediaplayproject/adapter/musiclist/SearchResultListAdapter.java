package com.example.mediaplayproject.adapter.musiclist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.SearchMusicBean;

import java.util.List;

/**
 * @author wm
 * @Classname MainListAdapter
 * @Date 2023/9/4 23:11
 * @Created by wm
 */
public class SearchResultListAdapter extends RecyclerView.Adapter<SearchResultListAdapter.ViewHolder>{

    private List<SearchMusicBean> musicList;
    private Context mContext;
    private int text_selected_color;
    private ColorStateList colors;

    public SearchResultListAdapter(Context context, List<SearchMusicBean> musicList) {
        
        this.mContext = context;
        this.musicList = musicList;
        Resources resources = mContext.getResources();
        // 文字选中的颜色
        text_selected_color = resources.getColor(R.color.text_pressed);
        // 文字未选中状态的selector
        colors = mContext.getResources().getColorStateList(R.color.listview_text_color_selector);
        resources = null;
    }

    public void setMusicList(List<SearchMusicBean> list) {
        this.musicList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchResultListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_music_list_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull SearchResultListAdapter.ViewHolder holder, int position) {
        SearchMusicBean searchMusicBean = musicList.get(position);
        holder.musicName.setText(searchMusicBean.getMusicTitle());
        holder.listName.setText(searchMusicBean.getSourceListName());
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView musicName;
        TextView listName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            musicName = itemView.findViewById(R.id.tv_music_name);
            listName = itemView.findViewById(R.id.tv_list_name);
        }
    }

}
