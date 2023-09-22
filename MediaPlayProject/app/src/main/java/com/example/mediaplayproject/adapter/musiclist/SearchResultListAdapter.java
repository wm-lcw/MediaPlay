package com.example.mediaplayproject.adapter.musiclist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.SearchMusicBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;

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
        colors = mContext.getResources().getColorStateList(R.color.search_view_text_color_selector);
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
        holder.musicArtist.setText("原唱 ：" + searchMusicBean.getSourceArtist());
        holder.listName.setText(searchMusicBean.getSourceListName());

        // 当前播放歌曲高亮显示
        long currentMusicId = DataRefreshService.getLastMusicId();
        String currentMusicList = DataRefreshService.getLastPlayListName();
        boolean isCurrentId = musicList.get(position).getMusicId() == currentMusicId;
        boolean isCurrentList = currentMusicList.equalsIgnoreCase(musicList.get(position).getSourceListName());
        if (isCurrentId && isCurrentList) {
            holder.musicName.setTextColor(text_selected_color);
        }  else {
            holder.musicName.setTextColor(colors);
        }

        holder.itemView.setOnClickListener(v -> {
            long musicId = musicList.get(position).getMusicId();
            String musicListName = musicList.get(position).getSourceListName();
            int playPosition = DataRefreshService.findPositionFromList(musicListName,musicId);
            Intent intent = new Intent(Constant.CHANGE_MUSIC_ACTION);
            Bundle bundle = new Bundle();
            bundle.putInt("position", playPosition);
            bundle.putString("musicListName", musicListName);
            intent.putExtras(bundle);
            mContext.sendBroadcast(intent);
        });
    }

    @Override
    public int getItemCount() {
        if (musicList == null) {
            return 0;
        } else {
            return musicList.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView musicName, musicArtist, listName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            musicName = itemView.findViewById(R.id.tv_music_name);
            musicArtist = itemView.findViewById(R.id.tv_music_artist);
            listName = itemView.findViewById(R.id.tv_list_name);
        }
    }

}
