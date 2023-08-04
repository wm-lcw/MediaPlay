package com.example.mediaplayproject.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;

/**
 * @author wm
 */
public class MusicAdapter extends BaseAdapter {
    private final Context mContext;
    private List<MediaFileBean> musicInfoList;

    private int defaultSelection = -1;
    private int text_selected_color;
    private int bg_selected_color;
    private ColorStateList colors;

    public MusicAdapter(Context mContext, List<MediaFileBean> musicInfoList) {
        this.mContext = mContext;
        this.musicInfoList = musicInfoList;
        Resources resources = mContext.getResources();
        // 文字选中的颜色
        text_selected_color = resources.getColor(R.color.text_pressed);
        // 背景选中的颜色
        bg_selected_color = resources.getColor(R.color.bg_selected);
        // 文字未选中状态的selector
        colors = mContext.getResources().getColorStateList(R.color.listview_text_color_selector);
        resources = null;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            //获取LayoutInflater实例
            convertView = LayoutInflater.from(mContext).inflate(R.layout.music_item,null);
            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tv_music_name);
            holder.ivLike = convertView.findViewById(R.id.iv_like_music);
            //将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            //convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tvName.setText(musicInfoList.get(position).getTitle());
        holder.ivLike.setImageResource(musicInfoList.get(position).isLike() ? R.mipmap.ic_list_like_choose : R.mipmap.ic_list_like);
        if (position == defaultSelection){
            //设置已选择选项的颜色
            holder.tvName.setTextColor(text_selected_color);
            //可以设置背景颜色，这里不设置
            //convertView.setBackgroundColor(bg_selected_color);
        } else {
            //设置按压效果
            holder.tvName.setTextColor(colors);
        }
        //监听item里面的收藏按钮事件，需要在自定义Adapter的getView方法首个参数前添加final关键字(final int position...)
        convertView.findViewById(R.id.iv_like_music).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取点击前的状态
                boolean isLike = musicInfoList.get(position).isLike();
                musicInfoList.get(position).setLike(!isLike);
                holder.ivLike.setImageResource(isLike ? R.mipmap.ic_list_like_choose : R.mipmap.ic_list_like);
                DebugLog.debug("position:" + position + "; isLike:" + isLike);
                DebugLog.debug("mediaFileBean " + musicInfoList.get(position));
                if (!isLike){
                    //加入收藏
                    DataRefreshService.addMusicToFavoriteList(musicInfoList.get(position));
                } else {
                    //取消收藏
                    DataRefreshService.deleteMusicFromFavoriteList(musicInfoList.get(position));
                }
                notifyItemChanged(position);
            }
        });
        return convertView;
    }

    public void notifyItemChanged(int position) {
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView tvName;
        ImageView ivLike;

    }

    public void setSelectPosition(int position){
        if (position == -1){
            //若传进来的值是-1，则代表要取消播放歌曲高亮效果
            defaultSelection = position;
            notifyDataSetChanged();
        }
        if (!(position < 0 || position > musicInfoList.size())) {
            defaultSelection = position;
            notifyDataSetChanged();
        }
    }

}
