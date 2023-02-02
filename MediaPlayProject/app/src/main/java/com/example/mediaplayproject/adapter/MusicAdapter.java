package com.example.mediaplayproject.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.MediaFileBean;

import java.util.List;

/**
 * @author wm
 */
public class MusicAdapter extends BaseAdapter {
    private final Context mContext;
    private List<MediaFileBean> musicInfoList;

    public MusicAdapter(Context mContext, List<MediaFileBean> musicInfoList) {
        this.mContext = mContext;
        this.musicInfoList = musicInfoList;
    }

    public MusicAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return musicInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            //获取LayoutInflater实例
            convertView = LayoutInflater.from(mContext).inflate(R.layout.music_item,null);
            holder = new ViewHolder();
            holder.tvName = (TextView) convertView.findViewById(R.id.tv_music_name);
            //将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            //convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvName.setText(musicInfoList.get(position).getTitle());
        return convertView;
    }

    public void notifyItemChanged(int i) {
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView tvName;
    }


}
