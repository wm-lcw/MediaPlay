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
import com.example.mediaplayproject.bean.LanguageBean;
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
public class LanguageChangeAdapter extends RecyclerView.Adapter<LanguageChangeAdapter.ViewHolder>{

    private Context mContext;
    private List<LanguageBean> languageBeans;
    private LanguageChangeAdapterListener mListener;

    private String currentSetLanguage, saveLanguage;

    public LanguageChangeAdapter() {
    }

    public LanguageChangeAdapter(Context mContext, List<LanguageBean> languageBeans) {
        this.mContext = mContext;
        this.languageBeans = languageBeans;
        currentSetLanguage = (String) SharedPreferencesUtil.getData(Constant.CURRENT_USE_LANGUAGE,"");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.change_language_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        saveLanguage = (String) SharedPreferencesUtil.getData(Constant.CURRENT_LANGUAGE,"");
        holder.languageName.setText(languageBeans.get(position).getLanguage());

        if (saveLanguage.equals(languageBeans.get(position).getLanguageTag())) {
            holder.languageCheckState.setImageResource(R.mipmap.ic_language_black_check);
        } else {
            holder.languageCheckState.setImageResource(R.mipmap.ic_language_black_nor);
        }

        holder.languageTip.setText("");

        holder.itemView.setOnClickListener(v -> {
            mListener.onClickItem(position);
        });
    }

    @Override
    public int getItemCount() {
        return languageBeans.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView languageName, languageTip;
        ImageView languageCheckState;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            languageName = itemView.findViewById(R.id.tv_language_name);
            languageTip = itemView.findViewById(R.id.tv_language_tip);
            languageCheckState = itemView.findViewById(R.id.iv_language_check_state);
        }
    }

    public interface LanguageChangeAdapterListener {
        /**
         *  点击语言的回调方法
         *  @author wm
         *  @createTime 2023/9/27 23:15
         * @param position: 选中语言的下标
         */
        void onClickItem(int position);
    }

    public void setLanguageChangeAdapterListener(LanguageChangeAdapterListener listener){
        mListener = listener;
    }
}
