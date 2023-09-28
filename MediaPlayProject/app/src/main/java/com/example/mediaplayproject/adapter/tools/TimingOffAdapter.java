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
import com.example.mediaplayproject.bean.LanguageBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;

import java.util.List;

/**
 * @author wm
 * @Classname AllToolsItemListAdapter
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/9/25 20:07
 * @Created by wm
 */
public class TimingOffAdapter extends RecyclerView.Adapter<TimingOffAdapter.ViewHolder>{

    private Context mContext;
    private List<String> timingOffList;
    private TimingOffAdapterListener mListener;

    private String timingOffType;

    public TimingOffAdapter() {
    }

    public TimingOffAdapter(Context mContext, List<String> timingOffList) {
        this.mContext = mContext;
        this.timingOffList = timingOffList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.timing_off_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        timingOffType = (String) SharedPreferencesUtil.getData(Constant.TIMING_OFF_TYPE,"关闭");
        holder.timingOffItemName.setText(timingOffList.get(position));

        if (timingOffType.equals(timingOffList.get(position))) {
            holder.timingOffItemCheckState.setImageResource(R.mipmap.ic_language_black_check);
        } else {
            holder.timingOffItemCheckState.setImageResource(R.mipmap.ic_language_black_nor);
        }

//        holder.itemView.setOnClickListener(v -> {
//            mListener.onClickItem(position);
//        });
    }

    @Override
    public int getItemCount() {
        return timingOffList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timingOffItemName;
        ImageView timingOffItemCheckState;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timingOffItemName = itemView.findViewById(R.id.tv_time_off_item_name);
            timingOffItemCheckState = itemView.findViewById(R.id.iv_timing_off_item_check_state);
        }
    }

    public interface TimingOffAdapterListener {
        /**
         *  选中定时关闭选项的回调方法
         *  @author wm
         *  @createTime 2023/9/27 23:15
         * @param position: 选中选项的下标
         */
        void onClickItem(int position);
    }

    public void setTimingOffAdapterListener(TimingOffAdapterListener listener){
        mListener = listener;
    }
}
