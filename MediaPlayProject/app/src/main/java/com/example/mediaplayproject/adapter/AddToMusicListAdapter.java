package com.example.mediaplayproject.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.MusicListBean;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;

/**
 * @author wm
 */
public class AddToMusicListAdapter extends BaseAdapter {
    private final Context mContext;
    private List<MusicListBean> musicInfoList;

    @SuppressLint("UseCompatLoadingForColorStateLists")
    public AddToMusicListAdapter(Context mContext, List<MusicListBean> musicList) {
        this.mContext = mContext;
        this.musicInfoList = musicList;
        DebugLog.debug("--" + this.musicInfoList);
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

    @SuppressLint({"InflateParams", "SetTextI18n"})
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.customer_music_list_item, null);
            holder = new ViewHolder();
            holder.ivListIcon = convertView.findViewById(R.id.iv_current_list);
            holder.tvCustomerListName = convertView.findViewById(R.id.tv_list_name);
            holder.tvCustomerListSize = convertView.findViewById(R.id.tv_list_size);
            holder.ivListPlaying = convertView.findViewById(R.id.iv_is_playing);
            holder.ivListSettings = convertView.findViewById(R.id.iv_customer_list_settings);
            //将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            // convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvCustomerListName.setText(musicInfoList.get(position).getListName());
        holder.tvCustomerListSize.setVisibility(View.GONE);
        holder.ivListPlaying.setVisibility(View.GONE);
        holder.ivListSettings.setVisibility(View.GONE);
        return convertView;
    }


    static class ViewHolder {
        TextView tvCustomerListName, tvCustomerListSize;
        ImageView ivListIcon, ivListPlaying, ivListSettings;
    }


}
