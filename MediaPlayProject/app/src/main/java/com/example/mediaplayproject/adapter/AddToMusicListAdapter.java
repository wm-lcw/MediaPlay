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
import com.example.mediaplayproject.utils.Constant;

import java.util.List;

/**
 * @author wm
 */
public class AddToMusicListAdapter extends BaseAdapter {
    private final Context mContext;
    private List<MusicListBean> musicInfoList;
    private MusicListBean favoriteList;

    @SuppressLint("UseCompatLoadingForColorStateLists")
    public AddToMusicListAdapter(Context mContext, List<MusicListBean> musicList) {
        this.mContext = mContext;
        this.musicInfoList = musicList;
        favoriteList = new MusicListBean(Constant.LIST_MODE_FAVORITE_NAME);
        if (!musicInfoList.contains(favoriteList)) {
            musicInfoList.add(favoriteList);
        }

    }

    /**
     *  更新自定义列表数据源
     *  @author wm
     *  @createTime 2023/9/9 11:18
     * @param musicList: 新的自定义列表
     */
    public void changeCustomerList(List<MusicListBean> musicList){
        this.musicInfoList = musicList;
        if (!musicInfoList.contains(favoriteList)) {
            musicInfoList.add(favoriteList);
        }
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.add_to_music_list_item, null);
            holder = new ViewHolder();
            holder.ivListIcon = convertView.findViewById(R.id.iv_list_icon);
            holder.tvCustomerListName = convertView.findViewById(R.id.tv_list_name);
            // 将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            // convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvCustomerListName.setText(musicInfoList.get(position).getListName());
        return convertView;
    }


    static class ViewHolder {
        TextView tvCustomerListName;
        ImageView ivListIcon;
    }


}
