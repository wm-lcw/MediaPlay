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
public class AllToolsItemListAdapter extends RecyclerView.Adapter<AllToolsItemListAdapter.ViewHolder>{

    private Context mContext;
    private List<ToolsBean> toolsBeanList;
    private AllToolsItemListAdapterListener mListener;
    /**
    * 编辑模式
    * */
    private boolean isEditMode = false;
    public AllToolsItemListAdapter() {
    }

    public AllToolsItemListAdapter(Context mContext, List<ToolsBean> toolsBeanList) {
        this.mContext = mContext;
        this.toolsBeanList = toolsBeanList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.all_tools_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.toolsAddIcon.setImageResource(R.mipmap.ic_tools_add_red);
        holder.toolsAddIcon.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.toolsIcon.setImageResource(toolsBeanList.get(position).getToolsIconId());
        holder.toolsName.setText(toolsBeanList.get(position).getToolsName());
        holder.itemView.setOnClickListener(v -> {
            if (!isEditMode) {
                mListener.onClickItemByAll(position);
            }
        });
        holder.toolsAddIcon.setOnClickListener(v -> {
            if (isEditMode){
                mListener.toolsAddIconOnClick(position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isEditMode) {
                mListener.onIntoEditModeByAll();
            }
            return false;
        });


    }

    @Override
    public int getItemCount() {
        return toolsBeanList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView toolsName;
        ImageView toolsAddIcon, toolsIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            toolsName = itemView.findViewById(R.id.tv_tools_name);
            toolsIcon = itemView.findViewById(R.id.iv_tools_icon);
            toolsAddIcon = itemView.findViewById(R.id.iv_tools_add);
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

    public interface AllToolsItemListAdapterListener {
        /**
         *  tools点击添加按钮的回调
         *  @author wm
         *  @createTime 2023/9/26 12:37
         * @param toolsId: toolsItem的Id，在所有工具列表中，可以看作是position
         */
        void toolsAddIconOnClick(int toolsId);


        /**
         *  普通模式下点击toolsItem的回调
         *  @author wm
         *  @createTime 2023/9/26 12:37
         * @param toolsId:toolsItem的Id
         */
        void onClickItemByAll(int toolsId);

        /**
         *  进入编辑模式
         *  @author wm
         *  @createTime 2023/9/26 15:25
         */
        void onIntoEditModeByAll();
    }

    public void setAllToolsItemListAdapterListener(AllToolsItemListAdapterListener listener){
        mListener = listener;
    }
}
