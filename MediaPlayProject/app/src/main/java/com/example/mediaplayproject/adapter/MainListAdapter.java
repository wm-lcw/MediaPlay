package com.example.mediaplayproject.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.MediaFileBean;

import java.util.List;

/**
 * @author wm
 * @Classname MainListAdapter
 * @Date 2023/9/4 23:11
 * @Created by wm
 */
public class MainListAdapter extends RecyclerView.Adapter<MainListAdapter.ViewHolder>{

    private List<MediaFileBean> musicList;
    private Context mContext;

    public MainListAdapter(Context context, List<MediaFileBean> musicList) {
        this.mContext = context;
        this.musicList = musicList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void changeList(List<MediaFileBean> newList){
        musicList = newList;
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
        holder.textView.setText(mediaFileBean.getTitle());
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
        }
    }
}
