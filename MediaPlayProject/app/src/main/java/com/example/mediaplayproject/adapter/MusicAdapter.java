package com.example.mediaplayproject.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
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
        if (position == defaultSelection){
            //设置已选择选项的颜色
            holder.tvName.setTextColor(text_selected_color);
            //可以设置背景颜色，这里不设置
            //convertView.setBackgroundColor(bg_selected_color);
        } else {
            //设置按压效果
            holder.tvName.setTextColor(colors);
        }
        return convertView;
    }

    public void notifyItemChanged(int i) {
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView tvName;
    }

    public void setSelectPosition(int position){
        if (!(position < 0 || position > musicInfoList.size())) {
            defaultSelection = position;
            notifyDataSetChanged();
        }
    }

}
