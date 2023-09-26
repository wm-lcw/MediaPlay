package com.example.mediaplayproject.adapter.tools;

import android.annotation.SuppressLint;
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
    /**
     * 编辑模式
     * */
    private boolean isEditMode = false;
    private ShortcutToolsItemListAdapterListener mListener;

    public ShortcutToolsItemListAdapter() {
    }

    public ShortcutToolsItemListAdapter(Context mContext, List<ToolsBean> toolsBeanList) {
        this.mContext = mContext;
        this.toolsBeanList = toolsBeanList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.shortcut_tools_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.toolsDeleteIcon.setImageResource(R.mipmap.ic_tools_delete_red);
        holder.toolsDeleteIcon.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        if (toolsBeanList.get(position).getToolsId() == -1){
            holder.toolsDeleteIcon.setVisibility(View.GONE);
        }
        holder.toolsIcon.setImageResource(toolsBeanList.get(position).getToolsIconId());
        holder.toolsName.setText(toolsBeanList.get(position).getToolsName());

        holder.itemView.setOnLongClickListener(v -> {
            if (!isEditMode) {
                mListener.onIntoEditModeByShort();
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (!isEditMode) {
                mListener.onClickItemByShortcut(toolsBeanList.get(position).getToolsId());
            }
        });
        holder.toolsDeleteIcon.setOnClickListener(v -> {
            if (isEditMode){
                mListener.toolsDeleteIconOnClick(toolsBeanList.get(position).getToolsId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return toolsBeanList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView toolsName;
        ImageView toolsDeleteIcon, toolsIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            toolsName = itemView.findViewById(R.id.tv_tools_name);
            toolsIcon = itemView.findViewById(R.id.iv_tools_icon);
            toolsDeleteIcon = itemView.findViewById(R.id.iv_tools_delete);
        }
    }

    /**
     *  设置编辑模式
     *  @author wm
     *  @createTime 2023/9/26 12:39
     * @param editMode:
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        notifyDataSetChanged();
    }

    /**
     *  设置数据列表
     *  @author wm
     *  @createTime 2023/9/26 12:40
     * @param toolsBeanList:
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setToolsBeanList(List<ToolsBean> toolsBeanList) {
        this.toolsBeanList = toolsBeanList;
        notifyDataSetChanged();
    }

    public interface ShortcutToolsItemListAdapterListener {
        /**
         *  tools点击删除按钮的回调
         *  @author wm
         *  @createTime 2023/9/26 12:37
         * @param toolsId: toolsItem的Id
         */
        void toolsDeleteIconOnClick(int toolsId);


        /**
         *  普通模式下点击toolsItem的回调
         *  @author wm
         *  @createTime 2023/9/26 12:37
         * @param toolsId:toolsItem的Id
         */
        void onClickItemByShortcut(int toolsId);

        /**
         *  进入编辑模式
         *  @author wm
         *  @createTime 2023/9/26 15:16
         */
        void onIntoEditModeByShort();
    }

    public void setShortcutToolsItemListAdapterListener(ShortcutToolsItemListAdapterListener listener){
        mListener = listener;
    }


}
