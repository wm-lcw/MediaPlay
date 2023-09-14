package com.example.mediaplayproject.adapter.musiclist;

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
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;

/**
 * @author wm
 * @Classname MusicListAdapter
 * @Version 1.0.0
 * @Date 2023/8/26 14:57
 * @Created by wm
 */
public class MusicListAdapter extends BaseAdapter {
    private final Context mContext;
    private List<MediaFileBean> musicInfoList;
    private int defaultSelection = -1;
    private int text_selected_color;
    private int bg_selected_color;
    private ColorStateList colors;
    private String listName;
    private int mPosition;

    public String getListName() {
        return listName;
    }

    public void changePlayList(List<MediaFileBean> list, String listName){
        this.listName = listName;
        this.musicInfoList = list;
        notifyDataSetChanged();
    }

    public void setListName(String listName) {
        this.listName = listName;
        notifyDataSetChanged();
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    public MusicListAdapter(Context mContext, String listName, List<MediaFileBean> musicList, int position) {
        this.mContext = mContext;
        this.listName = listName;
        this.musicInfoList = musicList;
        this.mPosition = position;
        defaultSelection = mPosition;
        Resources resources = mContext.getResources();
        // 文字选中的颜色
        text_selected_color = resources.getColor(R.color.text_pressed);
        // 背景选中的颜色
        bg_selected_color = resources.getColor(R.color.bg_selected);
        // 文字未选中状态的selector
        colors = mContext.getResources().getColorStateList(R.color.listview_text_color_selector);
        resources = null;
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.music_list_item, null);
            holder = new ViewHolder();
            holder.tvMusicName = convertView.findViewById(R.id.tv_music_name);
            holder.ivDeleteMusic = convertView.findViewById(R.id.iv_delete_music);
            //将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            // convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tvMusicName.setText(musicInfoList.get(position).getTitle());
        holder.ivDeleteMusic.setImageResource(R.mipmap.ic_delete);
        if (position == defaultSelection) {
            // 设置已选择选项的颜色
            holder.tvMusicName.setTextColor(text_selected_color);
        } else {
            holder.tvMusicName.setTextColor(colors);
        }
        if (listName.equals(Constant.LIST_MODE_DEFAULT_NAME)) {
            // 默认列表不支持删除操作
            holder.ivDeleteMusic.setEnabled(false);
            holder.ivDeleteMusic.setVisibility(View.GONE);
        } else {
            holder.ivDeleteMusic.setEnabled(true);
            holder.ivDeleteMusic.setVisibility(View.VISIBLE);
            // 监听item里面的删除按钮事件，需要在自定义Adapter的getView方法首个参数前添加final关键字(final int position...)
            convertView.findViewById(R.id.iv_delete_music).setOnClickListener(v -> {
                // 删除歌曲需要传递列表的具体信息
                DataRefreshService.deleteMusic(listName, position);
                notifyDataSetChanged();
            });
        }
        return convertView;
    }

    static class ViewHolder {
        TextView tvMusicName;
        ImageView ivDeleteMusic;
    }

    public void notifyItemChanged(int position) {
        notifyDataSetChanged();
    }

    /**
     *  选中高亮效果
     *  @author wm
     *  @createTime 2023/9/3 18:06
     * @param position: 需要高亮的歌曲下标
     */
    public void setSelectPosition(int position) {
        if (position == -1) {
            //若传进来的值是-1，则代表要取消播放歌曲高亮效果
            defaultSelection = position;
        }
        if (!(position < 0 || position > musicInfoList.size())) {
            defaultSelection = position;
            if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(listName)){
                defaultSelection = 0;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * @createTime 2023/2/11 14:56
     * @description 删除歌曲的时候，若删除的下标小于当前播放的下标，列表会上移，
     * 但是高亮的位置不变，导致了正在播放的高亮位置不正确，所以需要刷新指向高亮的下标
     */
    public void refreshSelectedPosition() {
        DebugLog.debug("defaultSelection " + defaultSelection);
        defaultSelection--;
        notifyDataSetChanged();
    }


    /**
     *  检查要删除的歌曲下标与当前播放歌曲下标的比较情况
     *  @createTime 2023/8/17 16:30
     * @param deletePosition: 要删除的歌曲下标
     * @return : int
     */
    public int checkRefreshPosition(int deletePosition) {
        if (deletePosition == defaultSelection) {
            // 删除的是当前播放的歌曲，先隐藏下标，等下一曲播放了再开启高亮下标
            return Constant.RESULT_IS_CURRENT_POSITION;
        } else if (deletePosition < defaultSelection) {
            // 删除的是下标小于当前的歌曲，需要刷新下标
            return Constant.RESULT_BEFORE_CURRENT_POSITION;
        } else {
            // 删除的是当前播放歌曲后面的，不受影响
            return Constant.RESULT_AFTER_CURRENT_POSITION;
        }
    }
}
