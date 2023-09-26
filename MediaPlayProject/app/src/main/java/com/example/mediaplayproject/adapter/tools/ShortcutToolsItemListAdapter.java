package com.example.mediaplayproject.adapter.tools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.bean.ToolsBean;

import java.util.List;

/**
 * @author wm
 * @Classname AllToolsItemListAdapter
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/9/25 20:07
 * @Created by wm
 */
public class ShortcutToolsItemListAdapter extends RecyclerView.Adapter<ShortcutToolsItemListAdapter.ViewHolder>{

    private Context mContext;
    private List<ToolsBean> toolsBeanList;
    public ShortcutToolsItemListAdapter() {
    }

    public ShortcutToolsItemListAdapter(Context mContext, List<ToolsBean> toolsBeanList) {
        this.mContext = mContext;
        this.toolsBeanList = toolsBeanList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.tools_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.toolsIcon.setImageResource(toolsBeanList.get(position).getToolsIconId());
        String name = toolsBeanList.get(position).getToolsName();
        holder.toolsName.setText(name);

    }

    @Override
    public int getItemCount() {
        return toolsBeanList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView toolsName;
        ImageView toolsIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            toolsName = itemView.findViewById(R.id.tv_tools_name);
            toolsIcon = itemView.findViewById(R.id.iv_tools_icon);
        }
    }
}
