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
import com.example.mediaplayproject.base.BasicApplication;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;

/**
 * @author wm
 */
public class FavoriteMusicAdapter extends BaseAdapter {
    private final Context mContext;
    private List<MediaFileBean> musicInfoList;

    private int defaultSelection = -1;
    private int text_selected_color;
    private int bg_selected_color;
    private ColorStateList colors;

    public FavoriteMusicAdapter(Context mContext, List<MediaFileBean> musicInfoList) {
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

    public FavoriteMusicAdapter(Context mContext) {
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.favorite_music_item, null);
            holder = new ViewHolder();
            holder.tvFavoriteName = convertView.findViewById(R.id.tv_favorite_music_name);
            holder.ivClose = convertView.findViewById(R.id.iv_delete_music);
            //将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            //convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tvFavoriteName.setText(musicInfoList.get(position).getTitle());
        holder.ivClose.setImageResource(R.mipmap.close);
        if (position == defaultSelection) {
            //设置已选择选项的颜色
            holder.tvFavoriteName.setTextColor(text_selected_color);
            //可以设置背景颜色，这里不设置
            //convertView.setBackgroundColor(bg_selected_color);
        } else {
            //设置按压效果
            holder.tvFavoriteName.setTextColor(colors);
        }
        //监听item里面的收藏按钮事件，需要在自定义Adapter的getView方法首个参数前添加final关键字(final int position...)
        convertView.findViewById(R.id.iv_delete_music).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取消收藏，这里调用deleteMusicFromFavoriteList删除收藏歌曲后，当前的收藏列表无需再删除，因为是同一对象
                BasicApplication.getApplication().deleteMusicFromFavoriteList(musicInfoList.get(position));
                DebugLog.debug("delete position " + position);
                notifyDataSetChanged();
            }
        });
        return convertView;
    }

    public void notifyItemChanged(int position) {
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView tvFavoriteName;
        ImageView ivClose;

    }

    public void setSelectPosition(int position) {
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
