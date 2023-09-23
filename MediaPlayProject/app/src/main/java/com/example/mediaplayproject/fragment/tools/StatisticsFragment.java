package com.example.mediaplayproject.fragment.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description: java类作用描述
 * @author: wm
 */
public class StatisticsFragment extends Fragment {

    private View myView;
    private Context mContext;
    private TextView tvPlayTotal, tvArtistTotal, tvPlayFromArtist, tvPlayTime;
    private ImageView ivBack, ivMore;
    private List<Map.Entry<String, Integer>> playTotalList = new ArrayList<>();
    private List<Map.Entry<String, Integer>> artistTotalList = new ArrayList<>();
    private List<Map.Entry<String, Integer>> mostPlayFromArtistTotalList = new ArrayList<>();

    public StatisticsFragment() {
    }

    public StatisticsFragment(Context context) {
        mContext = context;
    }

    @SuppressLint("StaticFieldLeak")
    private static StatisticsFragment instance;

    public static StatisticsFragment getInstance(Context context) {
        if (instance == null) {
            instance = new StatisticsFragment(context);
        }
        return instance;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_statistics, container, false);;
        initView();
        return myView;
    }

    private void initView() {
        ivBack = myView.findViewById(R.id.iv_statistics_back);
        ivMore = myView.findViewById(R.id.iv_statistics_more);
        tvPlayTotal = myView.findViewById(R.id.tv_play_total);
        tvArtistTotal = myView.findViewById(R.id.tv_artist_total);
        tvPlayFromArtist = myView.findViewById(R.id.tv_play_from_artist_total);
        tvPlayTime = myView.findViewById(R.id.tv_play_time);

        ivBack.setOnClickListener(mListener);
        ivMore.setOnClickListener(mListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDataFromService();
    }

    private void refreshDataFromService(){
        playTotalList = DataRefreshService.getPlayTotalList();
        artistTotalList = DataRefreshService.getArtistTotalList();
        mostPlayFromArtistTotalList = DataRefreshService.getMostPlayFromArtistTotalList(artistTotalList.get(0));
        toolsViewHandler.sendEmptyMessageDelayed(Constant.HANDLER_MESSAGE_DELAY_REFRESH_PLAY_TOTAL_DATA,300);
    }

    final Handler toolsViewHandler = new Handler(Looper.myLooper()) {
        @SuppressLint({"ResourceAsColor", "SetTextI18n"})
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == Constant.HANDLER_MESSAGE_DELAY_REFRESH_PLAY_TOTAL_DATA) {
                tvPlayTotal.setText("你播放最多的歌曲是 " + playTotalList.get(0).getKey()
                        + " ,\n\t    共播放了 " + playTotalList.get(0).getValue() + "次");
                tvArtistTotal.setText("你最喜欢的歌手是 " + artistTotalList.get(0).getKey()
                        + " ,\n\t    共播放了 " + artistTotalList.get(0).getValue() + "首TA的音乐");
                if (mostPlayFromArtistTotalList != null && mostPlayFromArtistTotalList.size() > 0) {
                    tvPlayFromArtist.setText("在" + artistTotalList.get(0).getKey() + "的歌曲中你最喜欢 "
                        + mostPlayFromArtistTotalList.get(0).getKey() + " ,\n\t    共播放了 "
                        + mostPlayFromArtistTotalList.get(0).getValue() + "次");
                }
                long count = DataRefreshService.getTotalPlayTime()/240;
                tvPlayTime.setText("你一共听了 " + DataRefreshService.getTotalPlayTime() + " 秒的音乐"
                        + " ,\n\t    约等于听了 " + count + " 首音乐");


            }
        }
    };

    private final View.OnClickListener mListener = view -> {
        if (view == ivBack) {
            // 返回主页
            Intent intent = new Intent(Constant.RETURN_MAIN_VIEW_ACTION);
            mContext.sendBroadcast(intent);
        } else if (view == ivMore) {
            // 更多功能

        }
    };


}