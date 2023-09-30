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

import java.util.List;

/**
 * @author wm
 * @Classname CustomerMusicListAdapter
 * @Version 1.0.0
 * @Date 2023/8/26 14:57
 * @Created by wm
 */
public class CustomerMusicListAdapter extends BaseAdapter {
    private final Context mContext;
    private List<MusicListBean> musicInfoList;
    private OnImageViewClickListener mListener;
    private String currentPlayingListName;

    @SuppressLint("UseCompatLoadingForColorStateLists")
    public CustomerMusicListAdapter(Context mContext, List<MusicListBean> musicList, String currentPlayingListName) {
        this.mContext = mContext;
        this.musicInfoList = musicList;
        this.currentPlayingListName = currentPlayingListName;
    }

    public void changeCustomerList(List<MusicListBean> musicList){
        this.musicInfoList = musicList;
        notifyDataSetChanged();
    }

    public void setCurrentPlayingListName(String musicListName) {
        this.currentPlayingListName = musicListName;
        notifyDataSetChanged();
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
            // 将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            // convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvCustomerListName.setText(musicInfoList.get(position).getListName());
        holder.tvCustomerListSize.setText(musicInfoList.get(position).getListSize() + "首");
        if (currentPlayingListName.equals(musicInfoList.get(position).getListName())){
            holder.ivListPlaying.setVisibility(View.VISIBLE);
        } else {
            holder.ivListPlaying.setVisibility(View.GONE);
        }

        convertView.findViewById(R.id.iv_customer_list_settings).setOnClickListener(v -> {
            // 对列表做操作
            if (mListener != null){
                mListener.onSettingImageViewClick(v, musicInfoList.get(position).getListName());
            }
        });

        return convertView;
    }



    static class ViewHolder {
        TextView tvCustomerListName, tvCustomerListSize;
        ImageView ivListIcon, ivListPlaying, ivListSettings;
    }

    public interface OnImageViewClickListener {

        /**
         *  ‘更多’按钮点击回调方法
         *  @author wm
         *  @createTime 2023/9/4 11:32
         *  @param view:
         *  @param listName:
         */
        void onSettingImageViewClick(View view,String listName);
    }

    public void setOnImageViewClickListener(OnImageViewClickListener listener){
        mListener = listener;
    }

}
