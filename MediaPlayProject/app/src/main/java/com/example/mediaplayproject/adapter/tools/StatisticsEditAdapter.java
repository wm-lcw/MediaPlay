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

import java.util.List;

/**
 * @author wm
 * @Date 2023/11/17 19:24
 */
public class StatisticsEditAdapter extends RecyclerView.Adapter<StatisticsEditAdapter.ViewHolder> {
    private Context mContext;
    private List<String> mList;
    private int checkItemSub = 0;
    private StatisticsEditCallBack mEditCallBack;

    public StatisticsEditAdapter(Context context, List<String> list) {
        mContext = context;
        mList = list;
    }

    public int getCheckItemSub() {
        return checkItemSub;
    }

    public void setCheckItemSub(int checkItemSub) {
        this.checkItemSub = checkItemSub;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatisticsEditAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.statistics_edit_item_view,parent,false);
        return new ViewHolder(view);
    }

    @Override
    @SuppressLint("RecyclerView")
    public void onBindViewHolder(@NonNull StatisticsEditAdapter.ViewHolder holder,  int position) {
        holder.tvStatisticsText.setText(mList.get(position));
        if (position == checkItemSub){
            holder.ivStatisticsCheck.setImageResource(R.mipmap.ic_language_black_check);
        } else {
            holder.ivStatisticsCheck.setImageResource(R.mipmap.ic_language_black_nor);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCheckItemSub(position);
                mEditCallBack.onClickItem(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatisticsText;
        ImageView ivStatisticsCheck;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatisticsText = itemView.findViewById(R.id.tv_statistics_text);
            ivStatisticsCheck = itemView.findViewById(R.id.iv_statistics_check_state);
        }
    }

    public interface StatisticsEditCallBack{
        /**
         *  点击回调方法
         *  @author wm
         *  @createTime 2023/11/17 19:47
         * @param position:
         */
        void onClickItem(int position);
    }

    public void setEditCallBack(StatisticsEditCallBack editCallBack) {
        mEditCallBack = editCallBack;
    }
}
